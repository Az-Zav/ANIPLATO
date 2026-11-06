package com.aniplato.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A food group's documentation of one completed handoff. "Late" is worked out from the offer's receipt time, not stored (A9). */
public class Report {
    public int id;
    public int offerId;
    public String eventName;
    public LocalDate eventDate;
    public int peopleServed;
    public String description;
    public LocalDateTime photoConsentAt;
    public LocalDateTime submittedAt;
    public List<String> photos = new ArrayList<>();   // stored paths, filled by the service

    /** Submitted after the deadline (receipt time plus the report period). */
    public boolean isLate(LocalDateTime deadline) {
        return submittedAt.isAfter(deadline);
    }
}
