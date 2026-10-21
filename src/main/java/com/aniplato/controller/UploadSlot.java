package com.aniplato.controller;

import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Document;
import com.aniplato.model.DocumentUpload;
import com.aniplato.service.FileStorageService;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/**
 * One document row on a form: its file pill, upload button, expiry picker and, where the row has one, the
 * No expiry box. It opens the file explorer and shows the chosen file; the rules stay in the services.
 */
public final class UploadSlot {

    private static File lastFolder;   // the file explorer reopens where the last upload came from

    private final FileStorageService fileStorageService = new FileStorageService();
    private final HBox fileBox;
    private final Label fileLabel;
    private final Button uploadBtn;
    private final DatePicker expiryPicker;
    private final CheckBox noExpiryCheck;   // null for rows that always need a date
    private Document.Type type;             // fixed for permits and certificates; follows the combo for the credential
    private File file;

    public UploadSlot(Document.Type type, HBox fileBox, Label fileLabel, Button uploadBtn,
                      DatePicker expiryPicker, CheckBox noExpiryCheck) {
        this.type = type;
        this.fileBox = fileBox;
        this.fileLabel = fileLabel;
        this.uploadBtn = uploadBtn;
        this.expiryPicker = expiryPicker;
        this.noExpiryCheck = noExpiryCheck;
    }

    public Document.Type type() {
        return type;
    }

    public void setType(Document.Type type) {
        this.type = type;
    }

    public DatePicker expiryPicker() {
        return expiryPicker;
    }

    public boolean hasNoExpiryBox() {
        return noExpiryCheck != null;
    }

    /**
     * Opens the system file explorer; a valid PDF, JPG or PNG replaces the row's file. Cancel changes nothing.
     * Returns the message to show when the file is refused (R1.1), or null.
     */
    public String choose(Window owner, String documentName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose your " + documentName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF, JPG or PNG", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        if (lastFolder != null && lastFolder.isDirectory()) chooser.setInitialDirectory(lastFolder);
        File chosen = chooser.showOpenDialog(owner);
        if (chosen == null) return null;
        try {
            fileStorageService.validate(chosen);   // type and size, checked now so the user hears about it at once
        } catch (RuleViolationException e) {
            return documentName + ": " + e.getMessage();   // A8
        }
        lastFolder = chosen.getParentFile();
        show(chosen);
        return null;
    }

    /** Coming back from step 1: shows the file and date the user had already chosen. */
    public void restore(DocumentUpload upload) {
        show(upload.file());
        if (noExpiryCheck != null) {
            noExpiryCheck.setSelected(upload.noExpiry());
            expiryPicker.setDisable(upload.noExpiry());
        }
        expiryPicker.setValue(upload.expiresOn());
    }

    /** Empties the row: no file, the upload button back. */
    public void clear() {
        file = null;
        fileBox.setVisible(false);
        fileBox.setManaged(false);
        uploadBtn.setVisible(true);
        uploadBtn.setManaged(true);
    }

    /** What the row holds, or null while it has no file or no document type yet. */
    public DocumentUpload toUpload() {
        if (file == null || type == null) return null;
        boolean noExpiry = noExpiryCheck != null && noExpiryCheck.isSelected();
        return new DocumentUpload(type, file, expiryPicker.getValue(), noExpiry);
    }

    private void show(File chosen) {
        file = chosen;
        fileLabel.setText(chosen.getName());
        fileLabel.setTooltip(new Tooltip(chosen.getName()));   // the label shortens long names; hover shows all
        fileBox.setVisible(true);
        fileBox.setManaged(true);
        uploadBtn.setVisible(false);
        uploadBtn.setManaged(false);
    }
}
