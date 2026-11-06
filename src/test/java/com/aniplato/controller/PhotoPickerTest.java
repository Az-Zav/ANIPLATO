package com.aniplato.controller;

import com.aniplato.support.Fx;
import com.aniplato.util.Settings;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The photo tiles: each pick adds to the photos already chosen, one grey tile waits for the next, and a ✕ takes one out. */
class PhotoPickerTest {

    @TempDir
    Path folder;

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private File picture(String name) throws IOException {
        File file = folder.resolve(name).toFile();
        ImageIO.write(new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", file);
        return file;
    }

    private List<File> pictures(int count, String prefix) throws IOException {
        List<File> made = new ArrayList<>();
        for (int i = 0; i < count; i++) made.add(picture(prefix + i + ".png"));
        return made;
    }

    private static PhotoPicker picker() {
        return Fx.call(PhotoPicker::new);
    }

    private static long count(PhotoPicker picker, String styleClass) {
        return Fx.call(() -> picker.lookupAll("." + styleClass).size());
    }

    private static String statusLine(PhotoPicker picker) {
        return Fx.call(() -> picker.getChildren().stream().filter(Label.class::isInstance)
                .map(n -> ((Label) n).getText()).findFirst().orElseThrow());
    }

    @Test
    void aSecondPickAddsToTheFirstInsteadOfReplacingIt() throws IOException {
        PhotoPicker picker = picker();
        File a = picture("a.png");
        File b = picture("b.png");
        File c = picture("c.png");

        Fx.run(() -> picker.add(List.of(a, b)));
        Fx.run(() -> picker.add(List.of(c)));

        assertEquals(List.of(a, b, c), picker.files());
        assertEquals("3 photos selected.", statusLine(picker));
        assertEquals(3, count(picker, "photo-remove"), "a ✕ on each preview");
    }

    @Test
    void aPictureAlreadyChosenIsNotAddedTwice() throws IOException {
        PhotoPicker picker = picker();
        File a = picture("a.png");
        Fx.run(() -> picker.add(List.of(a)));
        Fx.run(() -> picker.add(List.of(a)));
        assertEquals(List.of(a), picker.files());
        assertEquals("1 photo selected.", statusLine(picker));
    }

    @Test
    void oneGreyTileWaitsForTheNextPictureAndItGoesAtTheLimit() throws IOException {
        PhotoPicker picker = picker();
        assertEquals(1, count(picker, "photo-blank"));
        assertEquals(1, count(picker, "photo-add"));
        assertEquals("No photos selected.", statusLine(picker));

        List<File> four = pictures(Settings.MAX_PHOTOS - 1, "p");
        Fx.run(() -> picker.add(four));
        assertEquals(1, count(picker, "photo-blank"), "still just one grey tile");
        assertEquals(1, count(picker, "photo-add"));

        List<File> last = pictures(1, "last");
        Fx.run(() -> picker.add(last));
        assertEquals(0, count(picker, "photo-blank"), "no room left");
        assertEquals(0, count(picker, "photo-add"), "the Add tile goes too");
        assertEquals(Settings.MAX_PHOTOS, picker.files().size());
    }

    @Test
    void picturesBeyondTheLimitAreLeftOutAndSaid() throws IOException {
        PhotoPicker picker = picker();
        List<File> first = pictures(3, "x");
        List<File> second = pictures(3, "y");
        Fx.run(() -> picker.add(first));
        int leftOut = Fx.call(() -> picker.add(second));

        assertEquals(1, leftOut);
        assertEquals(Settings.MAX_PHOTOS, picker.files().size());
        assertTrue(statusLine(picker).contains("1 was not added: at most 5 photos."), statusLine(picker));
    }

    @Test
    void theXButtonTakesThatPictureOutAndBringsBackTheAddTile() throws IOException {
        PhotoPicker picker = picker();
        List<File> five = pictures(Settings.MAX_PHOTOS, "z");
        Fx.run(() -> picker.add(five));
        assertEquals(0, count(picker, "photo-add"));

        Fx.run(() -> ((Button) picker.lookupAll(".photo-remove").stream().skip(1).findFirst().orElseThrow()).fire());   // the second one

        assertEquals(List.of(five.get(0), five.get(2), five.get(3), five.get(4)), picker.files());
        assertEquals("4 photos selected.", statusLine(picker));
        assertEquals(1, count(picker, "photo-add"));
        assertEquals(1, count(picker, "photo-blank"));
    }

    @Test
    void takingOutTheLastPictureLeavesNoPhotosSelected() throws IOException {
        PhotoPicker picker = picker();
        File a = picture("a.png");
        Fx.run(() -> picker.add(List.of(a)));
        Fx.run(() -> picker.remove(a));
        assertTrue(picker.files().isEmpty());
        assertEquals("No photos selected.", statusLine(picker));
    }
}
