package com.aniplato.model;

import java.time.LocalDateTime;

/** A concern raised on an accepted or completed offer. The flagged account is the other side of the offer. */
public class SafetyNotice {
    public enum Status { OPEN, DISMISSED, ACCOUNT_SUSPENDED }

    public int id;                          // shown as "Notice #12"
    public int offerId;
    public int raisedBy;                    // account id of whoever flagged the concern
    public String concern;
    public Status status;
    public String adminNote;                // null until resolved; required on resolve
    public Integer resolvedBy;              // admin's account id; null until resolved
    public LocalDateTime resolvedAt;        // null until resolved
    public LocalDateTime raisedAt;

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** How the notice is named on screens. */
    public String label() {
        return "Notice #" + id;
    }
}
