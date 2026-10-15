package com.aniplato.service;

import com.aniplato.dao.ListingDao;
import com.aniplato.dao.LogDao;
import com.aniplato.exception.DataAccessException;
import com.aniplato.model.Listing;
import com.aniplato.util.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Post, edit, cancel and expire listings. */
public class ListingService {

    private static final String ENTITY = "LISTING";

    private final ListingDao listingDao = new ListingDao();
    private final LogDao logDao = new LogDao();

    /** Maintenance transition for Open listings past their use-by time (6.6, A3). */
    public void expireOverdueOpenListings(LocalDateTime now) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Integer> ids = listingDao.findOpenPastUseBy(c, now);
                for (int id : ids) {
                    if (listingDao.expireOpenIfPastUseBy(c, id, now) > 0) {
                        logDao.logStatusChange(c, ENTITY, id, Listing.Status.OPEN.name(),
                                Listing.Status.EXPIRED.name(), "Use-by time passed", null);
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not expire overdue listings", e);
        }
    }
}
