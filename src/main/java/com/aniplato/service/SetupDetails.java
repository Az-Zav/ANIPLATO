package com.aniplato.service;

import com.aniplato.exception.RuleViolationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the person typed on the Setup screen (6.8). A blank CA file means no certificate check (a database on the same computer,
 * for example). Build the JDBC addresses here so the rules about names and addresses live in one place.
 */
public record SetupDetails(String host, String port, String database, String user, String password, String caFile) {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,64}");
    private static final Pattern URL = Pattern.compile("^jdbc:mysql://([^:/?\\s]+)(?::(\\d{1,5}))?/([A-Za-z0-9_]*)");
    private static final String LOCAL_OPTIONS = "?sslMode=PREFERRED&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila";

    /** Reads host, port and database out of a pasted connection URL such as jdbc:mysql://host:3306/aniplato. */
    public static SetupDetails fromUrl(String url, String user, String password, String caFile) {
        Matcher m = URL.matcher(url == null ? "" : url.trim());
        if (!m.find()) throw new RuleViolationException("That is not a MySQL address. It should look like jdbc:mysql://host:3306/aniplato.");
        return new SetupDetails(m.group(1), m.group(2) == null ? "3306" : m.group(2),
                m.group(3).isEmpty() ? "aniplato" : m.group(3), user, password, caFile);
    }

    SetupDetails checked() {
        if (blank(host) || host.trim().contains(" ")) throw new RuleViolationException("Enter the database host, for example localhost.");
        int portNumber;
        try {
            portNumber = Integer.parseInt(port == null ? "" : port.trim());
        } catch (NumberFormatException e) {
            portNumber = -1;
        }
        if (portNumber < 1 || portNumber > 65535) throw new RuleViolationException("The port must be a number such as 3306.");
        if (blank(database) || !NAME.matcher(database.trim()).matches()) {
            throw new RuleViolationException("The database name may use only letters, digits and underscores (up to 64).");
        }
        if (blank(user)) throw new RuleViolationException("Enter the database user name.");
        if (blank(password)) throw new RuleViolationException("Enter the database password. A blank password is not supported.");
        if (!blank(caFile) && !Files.isRegularFile(Path.of(caFile.trim()))) {
            throw new RuleViolationException("The certificate file '" + caFile.trim() + "' was not found.");
        }
        return new SetupDetails(host.trim(), String.valueOf(portNumber), database.trim(), user.trim(), password, blank(caFile) ? "" : caFile.trim());
    }

    boolean encrypted() {
        return !blank(caFile);
    }

    String serverUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + (encrypted() ? "" : LOCAL_OPTIONS);
    }

    String databaseUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + (encrypted() ? "" : LOCAL_OPTIONS);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
