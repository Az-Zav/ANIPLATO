package com.aniplato.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** An organization's details, from the moment it signs up. The login lives in Account. */
public class Registration {
    public enum Status { PENDING, APPROVED, REJECTED }

    public int id;
    public Account.Role role;           // DONOR or FOOD_GROUP, never ADMIN
    public String orgName;
    public Account.OrgType orgType;
    public City city;
    public String contactPerson;
    public String contactPhone;         // show only to the owner, the admin, or the other side of an accepted offer (A7)
    public String email;
    public Status status;
    public String rejectionReason;      // null unless rejected
    public Integer reviewedBy;          // admin's account id; null until reviewed
    public LocalDateTime reviewedAt;    // null until reviewed
    public LocalDateTime submittedAt;
    public List<Document> documents = new ArrayList<>();   // uploaded files, filled by the DAO
}
