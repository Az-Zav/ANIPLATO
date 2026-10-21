package com.aniplato.controller;

import com.aniplato.model.Document;
import com.aniplato.model.DocumentUpload;
import com.aniplato.support.Fx;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One document row of the registration form: what it shows and what it hands to the service. */
class UploadSlotTest {

    private final HBox fileBox = new HBox();
    private final Label fileLabel = new Label("sample.pdf");
    private final Button uploadBtn = new Button("Upload");
    private final DatePicker expiry = new DatePicker();
    private final CheckBox noExpiry = new CheckBox();

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private UploadSlot slot(Document.Type type, boolean withNoExpiryBox) {
        return Fx.call(() -> new UploadSlot(type, fileBox, fileLabel, uploadBtn, expiry, withNoExpiryBox ? noExpiry : null));
    }

    @Test
    void clearHidesTheFileAndBringsTheUploadButtonBack() {
        UploadSlot slot = slot(Document.Type.MAYORS_PERMIT, false);
        Fx.run(slot::clear);

        assertFalse(fileBox.isVisible());
        assertFalse(fileBox.isManaged());
        assertTrue(uploadBtn.isVisible());
        assertTrue(uploadBtn.isManaged());
        assertNull(Fx.call(slot::toUpload), "an empty row hands nothing to the service");
    }

    @Test
    void restoreShowsTheFileAndTheDateAndHandsThemBack() {
        UploadSlot slot = slot(Document.Type.MAYORS_PERMIT, false);
        LocalDate date = LocalDate.now().plusMonths(6);
        File file = new File("permit.pdf");

        Fx.run(() -> slot.restore(new DocumentUpload(Document.Type.MAYORS_PERMIT, file, date, false)));

        assertEquals("permit.pdf", fileLabel.getText());
        assertTrue(fileBox.isVisible());
        assertFalse(uploadBtn.isVisible());
        assertFalse(uploadBtn.isManaged());
        DocumentUpload back = Fx.call(slot::toUpload);
        assertEquals(new DocumentUpload(Document.Type.MAYORS_PERMIT, file, date, false), back);
    }

    @Test
    void theNoExpiryBoxIsRestoredAndDisablesTheDatePicker() {
        UploadSlot slot = slot(Document.Type.AUTHORIZATION_LETTER, true);
        File file = new File("letter.pdf");

        Fx.run(() -> slot.restore(new DocumentUpload(Document.Type.AUTHORIZATION_LETTER, file, null, true)));

        assertTrue(slot.hasNoExpiryBox());
        assertTrue(noExpiry.isSelected());
        assertTrue(expiry.isDisabled());
        assertTrue(Fx.call(slot::toUpload).noExpiry());
    }

    @Test
    void aRowWithoutADocumentTypeHandsNothingEvenWithAFile() {
        UploadSlot slot = slot(null, true);   // the credential row, before a credential is chosen
        Fx.run(() -> slot.restore(new DocumentUpload(Document.Type.DSWD, new File("x.pdf"), null, true)));

        assertNull(Fx.call(slot::toUpload));

        Fx.run(() -> slot.setType(Document.Type.DSWD));
        assertEquals(Document.Type.DSWD, Fx.call(slot::toUpload).type());
    }

    @Test
    void aRowWithoutTheBoxNeverReportsNoExpiry() {
        UploadSlot slot = slot(Document.Type.BARANGAY_CERTIFICATE, false);
        Fx.run(() -> slot.restore(new DocumentUpload(Document.Type.BARANGAY_CERTIFICATE, new File("b.pdf"),
                LocalDate.now().plusYears(1), true)));

        assertFalse(slot.hasNoExpiryBox());
        assertFalse(Fx.call(slot::toUpload).noExpiry());
    }
}
