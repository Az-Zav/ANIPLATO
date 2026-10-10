package com.aniplato.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One uploaded file. A renewal is a new row, never an edit. */
public class Document {
    public enum Type {
        DTI("DTI Registration"),
        SEC("SEC Registration"),
        CDA("CDA Registration"),
        MAYORS_PERMIT("Mayor's Permit"),
        SANITARY_PERMIT("Sanitary Permit"),
        FDA_LTO("FDA License to Operate"),
        DSWD("DSWD Accreditation"),
        PCNC("PCNC Certification"),
        SEC_NON_STOCK("SEC Non-Stock Registration"),
        DEPED_CHED_TESDA("DepEd/CHED/TESDA Recognition"),
        DIOCESE_ENDORSEMENT("Diocese Endorsement"),
        AUTHORIZATION_LETTER("Authorization Letter");

        //** Label field inside Type enum */
        private final String label;

        //**enum constructor */
        Type(String label) {
            this.label = label;
        }

        /** Text shown on screens. */
        public String label() {
            return label;
        }

        //---ENUM METHODS---

        /** Value stored in the database, which is lowercase: MAYORS_PERMIT becomes mayors_permit. */
        public String dbValue() {
            return name().toLowerCase();
        }

        /** Reads a database value back into the enum. */
        public static Type fromDb(String value) {
            return valueOf(value.toUpperCase());
        }
    }

    public enum Review { OK, FLAGGED }

    public int id;
    public int registrationId;          // which registration this file belongs to
    public Type type;
    public String filePath;             // relative to the uploads/ folder
    public LocalDate expiresOn;         // null means the document has no expiry
    public Review review;               // null means awaiting the admin's review
    public String flagReason;           // null unless flagged; goes into the rejection email
    public LocalDateTime uploadedAt;
    public Integer reviewedBy;          // admin's account id; null until reviewed
    public LocalDateTime reviewedAt;    // null until reviewed

    /** True once the expiry date is before today. A document with no expiry never expires. */
    public boolean isExpired(LocalDate today) {
        return expiresOn != null && expiresOn.isBefore(today);
    }

    /** True if still valid today but expiring within the given number of days (the 30-day warning). */
    public boolean expiresWithin(int days, LocalDate today) {
        return expiresOn != null && !isExpired(today) && !expiresOn.isAfter(today.plusDays(days));
    }

    public boolean isAwaitingReview() {
        return review == null;
    }
}
 