package com.aniplato.service;

import com.aniplato.support.DbTest;
import com.aniplato.support.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uploads live in the database: every user gets the same bytes, a big photo is shrunk, a refused form leaves nothing behind. */
class FileStorageServiceTest extends DbTest {

    @TempDir
    Path tmp;

    private final FileStorageService files = new FileStorageService();

    private long stored() {
        return TestDb.<Long>scalar("SELECT COUNT(*) FROM stored_file");
    }

    private File image(String name, int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) img.setRGB(x, y, ((x / 40 * 40) << 16) | ((y / 40 * 40) << 8));   // blocks of colour: compresses well, so the file stays small
        File file = tmp.resolve(name).toFile();
        ImageIO.write(img, name.endsWith("png") ? "png" : "jpg", file);
        return file;
    }

    @Test
    void aSavedFileIsInTheDatabaseAndComesBackAsTheSameBytes() throws IOException {
        File pdf = Files.write(tmp.resolve("permit.pdf"), "%PDF-1.4 hello".getBytes()).toFile();
        String path = files.save(pdf);
        assertEquals(1, stored());
        assertTrue(path.endsWith(".pdf"));
        assertArrayEquals(Files.readAllBytes(pdf.toPath()), Files.readAllBytes(files.resolve(path).toPath()));
    }

    @Test
    void anotherComputerWithNoLocalCopyGetsTheFileFromTheDatabase() throws IOException {
        String path = files.save(Files.write(tmp.resolve("a.pdf"), "%PDF-1.4 shared".getBytes()).toFile());
        File cached = files.resolve(path);
        assertTrue(cached.delete(), "forget the local copy, as on a teammate's machine");
        assertTrue(files.resolve(path).isFile(), "it is fetched again from the database");
    }

    @Test
    void aLargePhotoIsShrunkToTheLimitAndSavedAsJpeg() throws IOException {
        File big = image("big.png", 3200, 2000);
        String path = files.save(big);
        assertTrue(path.endsWith(".jpg"));
        BufferedImage back = ImageIO.read(files.resolve(path));
        assertEquals(1600, back.getWidth());
        assertEquals(1000, back.getHeight());
    }

    @Test
    void aSmallPhotoIsKeptAsItIs() throws IOException {
        File small = image("small.jpg", 200, 150);
        String path = files.save(small);
        assertArrayEquals(Files.readAllBytes(small.toPath()), Files.readAllBytes(files.resolve(path).toPath()));
    }

    @Test
    void aFileTheDatabaseDoesNotHaveIsNotAFile() {
        assertFalse(files.resolve("no-such-file.jpg").isFile());
    }

    @Test
    void deletingRemovesTheStoredFileAndItsLocalCopy() throws IOException {
        String path = files.save(Files.write(tmp.resolve("b.pdf"), "%PDF".getBytes()).toFile());
        File cached = files.resolve(path);
        files.deleteAll(List.of(path));
        assertEquals(0, stored());
        assertFalse(cached.isFile());
    }
}
