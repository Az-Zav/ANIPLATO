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

    @Test
    void withPrefixReturnsOnlyThoseSettingsAndHonoursOverrides() {
        System.setProperty("aniplato.mail.zz.test", "yes");
        try {
            java.util.Properties mail = AppConfig.withPrefix("mail.");
            assertEquals("yes", mail.getProperty("mail.zz.test"));
            assertTrue(mail.stringPropertyNames().stream().allMatch(k -> k.startsWith("mail.")),
                    "no database or other settings leak into the mail library");
        } finally {
            System.clearProperty("aniplato.mail.zz.test");
        }
    }

    @Test
    void writtenSettingsReadBackExactlyEvenWithAwkwardPasswords(@org.junit.jupiter.api.io.TempDir Path folder) throws Exception {
        Path file = folder.resolve("deeper").resolve("db.properties");   // the folder does not exist yet
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("url", "jdbc:mysql://localhost:3306/aniplato?useSSL=false&serverTimezone=Asia/Manila");
        values.put("user", "root");
        values.put("password", " p@ss:w=rd #1 ! \\ é ");
        values.put("ssl.ca", "C:/certs/ca.pem");
        AppConfig.write(file, values);
        java.util.Properties back = new java.util.Properties();
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
            back.load(in);
        }
        assertEquals(values.keySet(), back.stringPropertyNames());
        for (java.util.Map.Entry<String, String> entry : values.entrySet()) {
            assertEquals(entry.getValue(), back.getProperty(entry.getKey()), entry.getKey());
        }
        java.util.List<String> lines = java.nio.file.Files.readAllLines(file);
        assertTrue(lines.get(1).startsWith("url="), "keys keep the order they were given: " + lines);
    }

    @Test
    void isConfiguredNeedsUrlUserAndPassword() {
        System.setProperty("aniplato.url", "jdbc:mysql://x/y");
        System.setProperty("aniplato.user", "u");
        System.setProperty("aniplato.password", " ");
        try {
            org.junit.jupiter.api.Assertions.assertFalse(AppConfig.isConfigured(), "a blank password is not a login");
            System.setProperty("aniplato.password", "p");
            assertTrue(AppConfig.isConfigured());
        } finally {
            System.clearProperty("aniplato.url");
            System.clearProperty("aniplato.user");
            System.clearProperty("aniplato.password");
        }
    }
}
