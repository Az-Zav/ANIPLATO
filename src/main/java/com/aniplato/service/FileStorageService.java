package com.aniplato.service;

import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.util.AppConfig;
import com.aniplato.util.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Saves uploads under the uploads folder (uploads.dir in db.properties, default uploads/) and returns paths relative to it. */
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("pdf", "jpg", "jpeg", "png");

    /** Rejects a missing file, a type other than PDF/JPG/PNG, or a file over the size limit (R1.1). */
    public void validate(File file) {
        if (file == null || !file.isFile()) {
            throw new RuleViolationException("Choose a file to upload.");
        }
        if (!ALLOWED.contains(extensionOf(file))) {
            throw new RuleViolationException("Upload a PDF, JPG or PNG file.");
        }
        if (file.length() > Settings.MAX_UPLOAD_MB * 1024L * 1024L) {
            throw new RuleViolationException("Each file must be " + Settings.MAX_UPLOAD_MB + " MB or smaller.");
        }
    }

    /** Validates, then copies the file to uploads/<uuid>.<ext>. Returns the relative path stored in the database. */
    public String save(File file) {
        validate(file);
        String name = UUID.randomUUID() + "." + extensionOf(file);
        try {
            Path root = root();
            Files.createDirectories(root);
            Files.copy(file.toPath(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DataAccessException("Could not save the uploaded file", e);
        }
        return name;
    }

    /**
     * Saves the photos of a post, offer or report: 1 to 5 files (6.7), each checked before any is copied. If one
     * fails part-way, the ones already copied are removed. Call {@link #deleteAll} if the database work that follows fails.
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

    /** Removes saved files, ignoring any that are already gone. Used when a transaction fails after the files were saved. */
    public void deleteAll(List<String> relativePaths) {
        for (String path : relativePaths) {
            try {
                Files.deleteIfExists(root().resolve(path));
            } catch (IOException ignored) {
                // a leftover upload is harmless; the database never pointed at it
            }
        }
    }

    /** The stored file for a relative path returned by {@link #save}. */
    public File resolve(String relativePath) {
        return root().resolve(relativePath).toFile();
    }

    private static Path root() {
        return Paths.get(AppConfig.get("uploads.dir", "uploads"));
    }

    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
