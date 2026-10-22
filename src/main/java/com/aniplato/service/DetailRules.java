package com.aniplato.service;

import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.FoodDetail;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** Checks shared by posts and offers: the same food detail, weight and attestation rules apply on both routes. */
final class DetailRules {

    static final double MAX_KG = 9999.99;   // post.kg and offer.kg are DECIMAL(6,2)

    private DetailRules() { }

    /** Checks the dates and attestation of a food detail and builds it. Shared with offers on a request (3B). */
    static FoodDetail foodDetail(String description, FoodDetail.Category category, FoodDetail.Storage storage,
                          Set<FoodDetail.Allergen> allergens, LocalDateTime useBy, LocalDateTime pickupStart,
                          LocalDateTime pickupEnd, String handover, boolean attested, LocalDateTime now) {
        if (useBy == null || !useBy.isAfter(now)) throw new RuleViolationException("Use-by must be in the future.");
        if (pickupStart == null || pickupEnd == null || !pickupStart.isBefore(pickupEnd)) {
            throw new RuleViolationException("The pickup window must end after it starts.");
        }
        if (!pickupEnd.isBefore(useBy)) throw new RuleViolationException("The pickup window must end before the use-by.");
        if (handover != null && handover.length() > 500) throw new RuleViolationException("Keep the handover instructions to 500 characters or fewer.");
        requireAttestation(attested);
        FoodDetail food = new FoodDetail();
        food.description = description.trim();
        food.category = category;
        food.storage = storage;
        food.allergens = allergens == null || allergens.isEmpty() ? Set.of() : EnumSet.copyOf(allergens);
        food.useBy = useBy;
        food.pickupStart = pickupStart;
        food.pickupEnd = pickupEnd;
        food.handoverInstructions = blankToNull(handover);
        food.attestedAt = now;
        return food;
    }

    static void requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) throw new RuleViolationException(message);
        if (value.trim().length() > maxLength) throw new RuleViolationException("Keep it to " + maxLength + " characters or fewer.");
    }

    static void requireKg(double kg) {
        if (!(kg > 0)) throw new RuleViolationException("The weight must be more than 0 kg.");
        if (kg > MAX_KG) throw new RuleViolationException("The weight must be " + MAX_KG + " kg or less.");
    }

    static void requireAttestation(boolean attested) {
        if (!attested) throw new RuleViolationException("Tick the attestation to continue.");
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
