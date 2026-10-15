package com.aniplato.service;

import com.aniplato.model.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Timed sweep. Calls other services only, never DAOs. */
public class MaintenanceService {

    private final OfferService offerService = new OfferService();
    private final ListingService listingService = new ListingService();
    private final RegistrationService registrationService = new RegistrationService();

    /** Runs the automatic checks in the order specified in section 6.6. */
    public void runMaintenance() {
        LocalDateTime now = LocalDateTime.now();
        offerService.lapseOverdueAcceptedOffers(now);
        listingService.expireOverdueOpenListings(now);
        offerService.expireOverdueOfferedOffers(now);
        registrationService.suspendAccountsWithExpiredDocuments(LocalDate.now());
    }

    /** Current user's expired or soon-to-expire documents for the Profile warning banner. */
    public List<Document> getCurrentDocumentAlerts() {
        return registrationService.getCurrentDocumentAlerts(LocalDate.now());
    }
}
