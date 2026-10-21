package com.aniplato.service;

import com.aniplato.exception.DataAccessException;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.util.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Saves uploads under uploads/ and returns paths relative to that folder. */
public class FileStorageService {

    private static final Path ROOT = Paths.get("uploads");
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
            Files.createDirectories(ROOT);
            Files.copy(file.toPath(), ROOT.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DataAccessException("Could not save the uploaded file", e);
        }
        return name;
    }

    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
