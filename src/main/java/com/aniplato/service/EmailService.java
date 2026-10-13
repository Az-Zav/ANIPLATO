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
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Sends the three emails and writes email_log.
 *
 * SMTP settings come from mail.properties on the classpath (next to db.properties): the standard
 * Jakarta Mail keys (mail.smtp.host, mail.smtp.port, mail.smtp.auth, mail.smtp.starttls.enable)
 * plus mail.user, mail.password and mail.from. If the file is missing or sending fails, the email
 * is logged with success = false and the action that triggered it still goes through.
 */
public class EmailService {

    /** email_log.kind. Each kind has its template under resources/email/. */
    public enum Kind {
        RECEIVED("received.html", "ANIPLATO: we received your registration"),
        APPROVED("approved.html", "ANIPLATO: your registration is approved"),
        NOT_APPROVED("not-approved.html", "ANIPLATO: your registration was not approved");

        private final String template;
        private final String subject;

        Kind(String template, String subject) {
            this.template = template;
            this.subject = subject;
        }
    }

    private static final String MAIL_CONFIG = "/mail.properties";
    private static final String TEMPLATE_ROOT = "/email/";
    private static final int MAX_ERROR_LENGTH = 300;   // email_log.error

    private final LogDao logDao = new LogDao();

    /** Confirms a submitted registration (R1). */
    public boolean sendReceived(String to, String orgName) {
        String message = "Thank you for registering " + orgName + " with ANIPLATO. "
                + "An admin will check your documents. We will email you the result. "
                + "No account exists yet, so there is nothing to log in to.";
        return send(Kind.RECEIVED, to, message);
    }

    /** Sends the temporary password of an approved registration (R1.5, R1.7). */
    public boolean sendApproved(String to, String orgName, String temporaryPassword) {
        String message = orgName + " is approved. Log in with this email and the temporary password: "
                + temporaryPassword + "\n\nYou will be asked to set your own password the first time you log in.";
        return send(Kind.APPROVED, to, message);
    }

    /** Sends the admin's reasons for a rejection (R1.6). */
    public boolean sendNotApproved(String to, String orgName, String reasons) {
        String message = "The registration of " + orgName + " was not approved for these reasons:\n\n"
                + reasons + "\n\nYou may fix them and register again.";
        return send(Kind.NOT_APPROVED, to, message);
    }

    /** Fills the template, sends it and logs the attempt. Returns whether it was sent. Never throws a mail error. */
    private boolean send(Kind kind, String to, String message) {
        String error = null;
        try {
            Properties config = loadConfig();
            String html = loadTemplate(kind).replace("{{message}}", toHtml(message));
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
            mail.setContent(html, "text/html; charset=UTF-8");
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

    private Properties loadConfig() throws IOException {
        try (InputStream in = EmailService.class.getResourceAsStream(MAIL_CONFIG)) {
            if (in == null) throw new IOException("Missing " + MAIL_CONFIG + " (SMTP settings)");
            Properties p = new Properties();
            p.load(in);
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
