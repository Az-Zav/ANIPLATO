package com.aniplato.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Rows the seed does not have, added through plain SQL so a test does not depend on the code under test. */
public final class Fixtures {

    private Fixtures() { }

    /** Small image files in the folder, to attach as photos. */
    public static List<File> photos(Path folder, int count) {
        try {
            List<File> files = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Path file = Files.write(folder.resolve("photo" + i + ".jpg"), new byte[]{1, 2, 3});
                files.add(file.toFile());
            }
            return files;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The seed's Open listing, posted by the Business donor. */
    public static int seedListingId() {
        return ((Number) TestDb.scalar("SELECT post_id FROM post WHERE kind = 'LISTING'")).intValue();
    }

    /** The seed's Open request, posted by the food group. */
    public static int seedRequestId() {
        return ((Number) TestDb.scalar("SELECT post_id FROM post WHERE kind = 'REQUEST'")).intValue();
    }

    /** An offer from the food group on the seed listing (Route 1). Returns its id. */
    public static int offerOnSeedListing(String status, double kg) {
        TestDb.exec("INSERT INTO use_detail (use_description, beneficiaries, headcount, place, due_at, attested_at) "
                + "VALUES ('Feeding program', 'Street children', 40, 'Quezon City', DATE_ADD(NOW(), INTERVAL 1 DAY), NOW())");
        int useDetail = ((Number) TestDb.scalar("SELECT MAX(use_detail_id) FROM use_detail")).intValue();
        TestDb.exec("INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, created_at) "
                        + "SELECT ?, account_id, ?, ?, ?, NOW() FROM account WHERE email = 'group@aniplato.test'",
                seedListingId(), kg, status, useDetail);
        return ((Number) TestDb.scalar("SELECT MAX(offer_id) FROM offer")).intValue();
    }

    /** An Open safety concern about an offer, raised by the given account. Returns its id. */
    public static int concern(int offerId, String raisedByEmail, String text) {
        TestDb.exec("INSERT INTO safety_notice (offer_id, kind, raised_by, concern, status, raised_at) "
                        + "SELECT ?, 'CONCERN', account_id, ?, 'OPEN', NOW() FROM account WHERE email = ?",
                offerId, text, raisedByEmail);
        return ((Number) TestDb.scalar("SELECT MAX(notice_id) FROM safety_notice")).intValue();
    }
}
