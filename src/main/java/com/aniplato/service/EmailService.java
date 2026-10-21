package com.aniplato.service;

import com.aniplato.dao.LogDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.util.Database;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.activation.DataHandler;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

/**
 * Sends the four emails and writes email_log.
 *
 * SMTP settings come from db.properties: the standard Jakarta Mail keys (mail.smtp.host,
 * mail.smtp.port, mail.smtp.auth, mail.smtp.starttls.enable) plus mail.user, mail.password and
 * mail.from. If they are missing or sending fails, the email is logged with success = false and
 * the action that triggered it still goes through.
 *
 * Templates use {{name}} placeholders; every value is HTML-escaped. The logo is sent inline as cid:logo@aniplato.
 */
public class EmailService {

    /** email_log.kind. Each kind has its template under resources/email/. */
    public enum Kind {
        RECEIVED("received.html", "ANIPLATO: we received your registration"),
        APPROVED("approved.html", "ANIPLATO: your registration is approved"),
        NOT_APPROVED("not-approved.html", "ANIPLATO: your registration was not approved"),
        PASSWORD_RESET("forgot-password.html", "ANIPLATO: your password was reset");

        private final String template;
        private final String subject;

        Kind(String template, String subject) {
            this.template = template;
            this.subject = subject;
        }
    }

    private static final String MAIL_CONFIG = "/db.properties";
    private static final String TEMPLATE_ROOT = "/email/";
    private static final String LOGO = "/assets/aniplato-logo-circle 1.png";
    private static final String LOGO_CID = "logo@aniplato";   // templates use src="cid:logo@aniplato"
    private static final int MAX_ERROR_LENGTH = 300;   // email_log.error

    private final LogDao logDao = new LogDao();

    /** Confirms a submitted registration (R1). */
    public boolean sendReceived(String to, String orgName) {
        return send(Kind.RECEIVED, to, Map.of("orgName", orgName));
    }

    /** Sends the temporary password of an approved registration (R1.5, R1.7). */
    public boolean sendApproved(String to, String orgName, String temporaryPassword) {
        return send(Kind.APPROVED, to, Map.of("orgName", orgName, "email", to, "password", temporaryPassword));
    }

    /** Sends the admin's reasons for a rejection (R1.6). */
    public boolean sendNotApproved(String to, String orgName, String reasons) {
        return send(Kind.NOT_APPROVED, to, Map.of("orgName", orgName, "reasons", reasons));
    }

    /** Sends a temporary password for a forgotten one (R1.7). */
    public boolean sendPasswordReset(String to, String temporaryPassword) {
        return send(Kind.PASSWORD_RESET, to, Map.of("email", to, "password", temporaryPassword));
    }

    /** Fills the template, sends it and logs the attempt. Returns whether it was sent. Never throws a mail error. */
    private boolean send(Kind kind, String to, Map<String, String> values) {
        String error = null;
        try {
            Properties config = loadConfig();
            String html = loadTemplate(kind);
            for (Map.Entry<String, String> v : values.entrySet()) {
                html = html.replace("{{" + v.getKey() + "}}", toHtml(v.getValue()));
            }
            Session mailSession = Session.getInstance(config, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getProperty("mail.user"), config.getProperty("mail.password"));
                }
            });
            MimeMessage mail = new MimeMessage(mailSession);
            mail.setFrom(new InternetAddress(config.getProperty("mail.from", config.getProperty("mail.user"))));
            mail.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            mail.setSubject(kind.subject, StandardCharsets.UTF_8.name());
            mail.setContent(withLogo(html));
            Transport.send(mail);
        } catch (MessagingException | IOException e) {
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }

        boolean success = error == null;
        if (error != null && error.length() > MAX_ERROR_LENGTH) error = error.substring(0, MAX_ERROR_LENGTH);
        try (Connection c = Database.getConnection()) {
            logDao.logEmail(c, to, kind.name(), LocalDateTime.now(), success, error);
        } catch (SQLException e) {
            throw new DataAccessException("Could not write the email log", e);
        }
        return success;
    }

    /** The HTML body plus the logo as an inline image the template shows with src="cid:logo@aniplato". */
    private MimeMultipart withLogo(String html) throws MessagingException, IOException {
        MimeMultipart related = new MimeMultipart("related; type=\"text/html\"");
        MimeBodyPart body = new MimeBodyPart();
        body.setContent(html, "text/html; charset=UTF-8");
        related.addBodyPart(body);

        try (InputStream in = EmailService.class.getResourceAsStream(LOGO)) {
            if (in == null) return related;   // send without the logo rather than not at all
            MimeBodyPart logo = new MimeBodyPart();
            logo.setDataHandler(new DataHandler(new ByteArrayDataSource(in.readAllBytes(), "image/png")));
            logo.setContentID("<" + LOGO_CID + ">");   // id@domain form: Gmail ignores a bare id
            logo.setDisposition(MimeBodyPart.INLINE);
            logo.setFileName("aniplato-logo.png");
            related.addBodyPart(logo);
        }
        return related;
    }

    private Properties loadConfig() throws IOException {
        try (InputStream in = EmailService.class.getResourceAsStream(MAIL_CONFIG)) {
            if (in == null) throw new IOException("Missing " + MAIL_CONFIG + " (SMTP settings)");
            Properties p = new Properties();
            p.load(in);
            for (String key : new String[] {"mail.smtp.host", "mail.user", "mail.password"}) {
                String value = p.getProperty(key);
                if (value == null || value.isBlank()) throw new IOException("Missing '" + key + "' in db.properties");
                p.setProperty(key, value.trim());
            }
            return p;
        }
    }

    private String loadTemplate(Kind kind) throws IOException {
        try (InputStream in = EmailService.class.getResourceAsStream(TEMPLATE_ROOT + kind.template)) {
            if (in == null) throw new IOException("Missing email template: " + kind.template);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Escapes user text (org names, reasons) and keeps line breaks. */
    private static String toHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");
    }
}
