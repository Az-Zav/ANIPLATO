package com.aniplato.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hosted-database link: the provider's PEM CA file becomes a truststore Connector/J can check the server against. */
class DatabaseTlsTest {

    private static Path truststore(Path pem, String password) throws Exception {
        Method m = Class.forName("com.aniplato.util.Database").getDeclaredMethod("truststoreFrom", Path.class, String.class);
        m.setAccessible(true);
        try {
            return (Path) m.invoke(null, pem, password);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void aPemCaFileBecomesAnOpenableTruststore() throws Exception {
        Path store = truststore(Path.of("src/test/resources/test-ca.pem"), "pw");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(store)) {
            ks.load(in, "pw".toCharArray());
        }
        assertEquals(1, ks.size());
        assertTrue(ks.isCertificateEntry("ca0"));
    }

    @Test
    void aFileThatIsNotACertificateIsSaidSoByName(@TempDir Path tmp) throws Exception {
        Path bad = Files.writeString(tmp.resolve("bad.pem"), "not a certificate");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> truststore(bad, "pw"));
        assertTrue(e.getMessage().contains("ssl.ca"), e.getMessage());
    }
}
