package com.aniplato.dao;

import com.aniplato.model.SafetyNotice;
import com.aniplato.support.DbTest;
import com.aniplato.support.Fixtures;
import com.aniplato.support.TestDb;
import com.aniplato.util.Database;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The kind column is written by the DAO, not left to the database's default. */
class SafetyNoticeDaoTest extends DbTest {

    private final SafetyNoticeDao dao = new SafetyNoticeDao();

    @Test
    void insertWritesTheKindAndFindByIdReadsItBack() throws SQLException {
        SafetyNotice notice = new SafetyNotice();
        notice.offerId = Fixtures.offerOnSeedListing("ACCEPTED", 5);
        notice.raisedBy = accountId(GROUP);
        notice.concern = "The bread was moldy.";
        notice.status = SafetyNotice.Status.OPEN;
        notice.raisedAt = LocalDateTime.now();

        try (Connection c = Database.getConnection()) {
            int id = dao.insert(c, notice);

            assertEquals("CONCERN", TestDb.scalar("SELECT kind FROM safety_notice WHERE notice_id = ?", id));
            assertEquals(SafetyNotice.Kind.CONCERN, dao.findById(c, id).kind);
        }
    }
}
