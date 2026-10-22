package com.aniplato.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/** What a food group needs or will do with the food: on a request (Route 2) or on an offer to a listing (Route 1). */
public class UseDetail {
    public int id;
    public String useDescription;                 // the event or program
    public LocalDate eventDate;                   // required on a request
    public String beneficiaries;
    public int headcount;
    public String place;
    public LocalDateTime dueAt;                   // needed-by on a request, pickup time on an offer
    public String message;
    public Set<FoodDetail.Category> acceptedCategories = Set.of();   // requests only
    public LocalDateTime attestedAt;
}
