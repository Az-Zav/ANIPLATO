package com.aniplato.service;

import com.aniplato.dao.StoredFileDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.util.AppConfig;
import com.aniplato.util.Database;
import com.aniplato.util.Settings;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploads (photos and documents) in the database, so every teammate sees the same files. A large photo is
 * shrunk to {@link Settings#PHOTO_MAX_PIXELS} and saved as JPEG. The path returned by {@link #save} is what the photo
 * and document rows hold; {@link #resolve} fetches the bytes once into a local cache folder and returns that file.
 */
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("pdf", "jpg", "jpeg", "png");

    private final StoredFileDao storedFiles = new StoredFileDao();

    /** Rejects a missing file, a type other than PDF/JPG/PNG, or a file over the size limit (R1.1). */
    public void validate(File file) {
        if (file == null || !file.isFile()) {
            throw new RuleViolationException("Choose a file to upload.");
        }
        String ext = extensionOf(file);
        if (!ALLOWED.contains(ext)) {
            throw new RuleViolationException("Upload a PDF, JPG or PNG file.");
        }
        long limitMb = ext.equals("pdf") ? Settings.MAX_PDF_MB : Settings.MAX_IMAGE_MB;
        if (file.length() > limitMb * 1024L * 1024L) {
            throw new RuleViolationException("Each file must be " + limitMb + " MB or smaller.");
        }
    }

    /** Validates, shrinks a large image, stores the bytes in the database. Returns the path the database rows hold. */
    public String save(File file) {
        validate(file);
        byte[] content;
        String ext = extensionOf(file);
        try {
            content = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new DataAccessException("Could not read the uploaded file", e);
        }
        if (!ext.equals("pdf")) {
            byte[] shrunk = shrink(content);
            if (shrunk != null) {
                content = shrunk;
                ext = "jpg";
            }
        }
        String name = UUID.randomUUID() + "." + ext;
        byte[] bytes = content;
        Database.inTransaction("Could not save the uploaded file", c -> {
            storedFiles.insert(c, name, bytes);
            return null;
        });
        return name;
    }

    /**
     * Saves the photos of a post, offer or report: 1 to 5 files (6.7), each checked before any is stored. If one
     * fails part-way, the ones already stored are removed. Call {@link #deleteAll} if the database work that follows fails.
     */
    public List<String> saveAllPhotos(List<File> files) {
        if (files == null || files.size() < Settings.MIN_PHOTOS || files.size() > Settings.MAX_PHOTOS) {
            throw new RuleViolationException("Add " + Settings.MIN_PHOTOS + " to " + Settings.MAX_PHOTOS + " photos.");
        }
        for (File file : files) validate(file);
        List<String> saved = new ArrayList<>();
        try {
            for (File file : files) saved.add(save(file));
        } catch (RuntimeException e) {
            deleteAll(saved);
            throw e;
        }
        return saved;
    }

    /** Removes stored files, ignoring any that are already gone. Used when a transaction fails after the files were saved. */
    public void deleteAll(List<String> paths) {
        for (String path : paths) {
            try {
                Database.inTransaction("Could not remove an uploaded file", c -> {
                    storedFiles.delete(c, path);
                    return null;
                });
                Files.deleteIfExists(cacheFile(path));
            } catch (IOException | RuntimeException ignored) {
                // a leftover upload is harmless; no photo or document row points at it
            }
        }
    }

    /**
     * The file for a stored path, on this computer: the cached copy, fetched from the database the first time. A file
     * the database does not have comes back as a File that does not exist ({@code isFile()} is false). A file in the
     * old uploads folder (uploads.dir) is still found there.
     */
    public File resolve(String path) {
        File legacy = Paths.get(AppConfig.get("uploads.dir", "uploads")).resolve(path).toFile();
        if (legacy.isFile()) return legacy;
        Path cached = cacheFile(path);
        if (Files.isRegularFile(cached)) return cached.toFile();
        byte[] content;
        try {
            content = Database.read("Could not load the file", c -> storedFiles.findContent(c, path));
        } catch (DataAccessException e) {
            return cached.toFile();   // no connection: the caller shows "not found"
        }
        if (content == null) return cached.toFile();
        try {
            Files.createDirectories(cached.getParent());
            Files.write(cached, content);
        } catch (IOException e) {
            throw new DataAccessException("Could not keep a copy of the file", e);
        }
        return cached.toFile();
    }

    private static Path cacheFile(String path) {
        String root = AppConfig.get("cache.dir", Paths.get(System.getProperty("java.io.tmpdir"), "aniplato-cache").toString());
        return Paths.get(root).resolve(path);
    }

    /** The image as a JPEG within the size limits, or null when it is already small enough or cannot be read as an image. */
    private static byte[] shrink(byte[] original) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
            if (image == null) return null;
            int longest = Math.max(image.getWidth(), image.getHeight());
            if (longest <= Settings.PHOTO_MAX_PIXELS && original.length <= Settings.PHOTO_TARGET_KB * 1024) return null;
            double scale = Math.min(1.0, (double) Settings.PHOTO_MAX_PIXELS / longest);
            int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
            BufferedImage flat = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = flat.createGraphics();
            g.setColor(Color.WHITE);   // a PNG's transparent parts would turn black in a JPEG
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, w, h, null);
            g.dispose();
            return jpeg(flat, 0.8f);
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] jpeg(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
