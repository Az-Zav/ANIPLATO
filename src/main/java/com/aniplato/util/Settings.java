package com.aniplato.util;

/** App settings (spec 6.7 and 7.3). Rules read these; no other class holds a literal for them. */
public final class Settings {
    private Settings() {}

    /** Accepted offer lapses this long after its pickup time (or pickup window end). */
    public static final int PICKUP_GRACE_HOURS = 2;
    /** The food group's report is due this many days after receipt. */
    public static final int REPORT_DEADLINE_DAYS = 7;
    /** Documents expiring within this many days show a warning. */
    public static final int DOCUMENT_WARNING_DAYS = 30;
    /** Photos per post, offer and report. */
    public static final int MIN_PHOTOS = 1;
    public static final int MAX_PHOTOS = 5;
    /** Upload limit for PDF, JPG and PNG files. */
    public static final int MAX_UPLOAD_MB = 5;
    /** New passwords must be at least this long. */
    public static final int MIN_PASSWORD_LENGTH = 8;
    /** Contact numbers are exactly this many digits (PH mobile, e.g. 09171234567). */
    public static final int CONTACT_NUMBER_DIGITS = 11;
}
