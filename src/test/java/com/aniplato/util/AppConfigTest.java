package com.aniplato.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a packaged app finds its settings, and how a file named in them (the CA) is resolved. */
class AppConfigTest {

    @Test
    void anAbsolutePathIsLeftAlone() {
        Path absolute = Paths.get("C:\\certs\\ca.pem").toAbsolutePath();
        assertEquals(absolute, AppConfig.path(absolute.toString()));
    }

    @Test
    void aRelativePathWithNoFileBesideTheSettingsStaysAsWritten() {
        assertEquals(Paths.get("no-such-file-anywhere.pem"), AppConfig.path("no-such-file-anywhere.pem"));
    }

    @Test
    void theExpectedSettingsLocationIsInTheUsersAppFolder() {
        String where = AppConfig.expectedLocation();
        assertTrue(where.endsWith("db.properties"), where);
        assertTrue(where.contains("ANIPLATO") || where.contains(".aniplato"), where);
    }

    @Test
    void aSystemPropertyStillWinsOverTheFile() {
        System.setProperty("aniplato.appconfig.test", "from-property");
        try {
            assertEquals("from-property", AppConfig.get("appconfig.test", "fallback"));
        } finally {
            System.clearProperty("aniplato.appconfig.test");
        }
        assertEquals("fallback", AppConfig.get("appconfig.test", "fallback"));
    }
}
