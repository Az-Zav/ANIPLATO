package com.aniplato.service;

import com.aniplato.dao.ListingDao;
import com.aniplato.dao.LogDao;
import com.aniplato.dao.OfferDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.model.Listing;
import com.aniplato.model.Offer;
import com.aniplato.util.Database;
import com.aniplato.util.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Make, accept, decline, withdraw offers; handoff transaction (R4.1, R5.1). */
public class OfferService {

    private static final String OFFER_ENTITY = "OFFER";
    private static final String LISTING_ENTITY = "LISTING";

    private final OfferDao offerDao = new OfferDao();
    private final ListingDao listingDao = new ListingDao();
    private final LogDao logDao = new LogDao();

    /** Accepted offers that missed the pickup grace period lapse with their listing (6.6, A2, A3). */
    public void lapseOverdueAcceptedOffers(LocalDateTime now) {
        LocalDateTime pickupCutoff = now.minusHours(Settings.PICKUP_GRACE_HOURS);
        inTransaction("Could not lapse overdue offers", c -> {
            List<Integer> ids = offerDao.findAcceptedPastGracePeriod(c, pickupCutoff);
            for (int id : ids) {
                Integer listingId = offerDao.findListingId(c, id);
                if (listingId != null && offerDao.lapseAcceptedIfPastGracePeriod(c, id, pickupCutoff) > 0) {
                    logDao.logStatusChange(c, OFFER_ENTITY, id, Offer.Status.ACCEPTED.name(),
                            Offer.Status.LAPSED.name(), "Pickup grace period passed", null);
                    if (listingDao.expireAccepted(c, listingId) > 0) {
                        logDao.logStatusChange(c, LISTING_ENTITY, listingId, Listing.Status.ACCEPTED.name(),
                                Listing.Status.EXPIRED.name(), "Accepted offer lapsed", null);
                    }
                }
            }
            return null;
        });
    }

    /** Offered offers whose pickup time passed expire without affecting their listing (6.6, A3). */
    public void expireOverdueOfferedOffers(LocalDateTime now) {
        inTransaction("Could not expire overdue offers", c -> {
            List<Integer> ids = offerDao.findOfferedPastPickupTime(c, now);
            for (int id : ids) {
                if (offerDao.expireOfferedIfPastPickupTime(c, id, now) > 0) {
                    logDao.logStatusChange(c, OFFER_ENTITY, id, Offer.Status.OFFERED.name(),
                            Offer.Status.EXPIRED.name(), "Pickup time passed", null);
                }
            }
            return null;
        });
    }

    private <T> T inTransaction(String message, Work<T> work) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException(message, e);
        }
    }

    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }
}
