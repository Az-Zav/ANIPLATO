-- ANIPLATO demo story. ADDITIVE: run once, after schema.sql and seed.sql (setup-db.ps1 -Demo does all three).
-- It only inserts. Every account has the password Aniplato123! (dev only). All times are relative to now,
-- so the story is the same whenever it is loaded. Copy sql/demo-photos/* to uploads/seed/ to see the photos.
--
-- Donor:   business@aniplato.test (Sunrise Bakery)      Food group: group@aniplato.test (Kapwa Community Kitchen)
-- Casual:  casual@aniplato.test                          Suspended:  suspended@aniplato.test (Pag-asa Feeding Circle)
USE aniplato;

SET @pw = '$2a$10$GA02ZoTIXq3qRsE2fg7rm.u3FQcJ1iWK2FiXQomukRmdxsB5kjSh.';
SET @admin    = (SELECT account_id FROM account WHERE email = 'admin@aniplato.test');
SET @business = (SELECT account_id FROM account WHERE email = 'business@aniplato.test');
SET @casual   = (SELECT account_id FROM account WHERE email = 'casual@aniplato.test');
SET @group    = (SELECT account_id FROM account WHERE email = 'group@aniplato.test');
SET @qc = (SELECT city_id FROM city WHERE name = 'Quezon City');
SET @mk = (SELECT city_id FROM city WHERE name = 'Makati');

-- A suspended food group (the admin's "reinstate" story) -------------------------------------------------
INSERT INTO registration (role, name, city_id, contact_person, contact_phone, email, status, reviewed_by, reviewed_at, submitted_at)
VALUES ('FOOD_GROUP', 'Pag-asa Feeding Circle', @mk, 'Luz Bautista', '09221234567', 'suspended@aniplato.test', 'APPROVED', @admin, NOW(), NOW());
SET @reg_sus = LAST_INSERT_ID();
INSERT INTO account (email, password_hash, role, status, suspend_reason, must_change_password, password_changed_at, registration_id, created_at)
VALUES ('suspended@aniplato.test', @pw, 'FOOD_GROUP', 'SUSPENDED', 'Report overdue and no reply to the admin', FALSE, NOW(), @reg_sus, NOW());
SET @suspended = LAST_INSERT_ID();
INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at, reviewed_by, reviewed_at) VALUES
 (@reg_sus, 'dswd',                 'seed/dswd.pdf', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW(), @admin, NOW()),
 (@reg_sus, 'authorization_letter', 'seed/auth.pdf', NULL,                                 'OK', NOW(), @admin, NOW());

-- ROUTE 1 -------------------------------------------------------------------------------------------------
-- L1: an Open listing with one offer waiting, so the donor has something to accept (sidebar badge 1)
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Fresh vegetables', 'RAW', 'CHILLED', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), NOW());
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 20.00, 'OPEN', LAST_INSERT_ID(), NOW());
SET @l1 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/vegetables.png', @l1);
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, message, attested_at)
VALUES ('Weekend feeding program', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Street children', 60, 'Kapwa Community Kitchen, Quezon City',
        DATE_ADD(NOW(), INTERVAL 36 HOUR), 'We can bring our own coolers.', NOW());
INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, created_at)
VALUES (@l1, @group, 20.00, 'OFFERED', LAST_INSERT_ID(), NOW());
INSERT INTO photo (file_path, offer_id) VALUES ('seed/event.png', LAST_INSERT_ID());

-- L2: Accepted, the donor still has to confirm release (the group sees it under My Offers)
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, handover_instructions, attested_at)
VALUES ('Cooked rice meals', 'COOKED', 'HOT_HOLD', DATE_ADD(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR),
        'Ask for Maria at the back counter.', NOW());
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 15.00, 'ACCEPTED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 5 HOUR));
SET @l2 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/rice.png', @l2);
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, attested_at)
VALUES ('Parish lunch', CURDATE(), 'Elderly parishioners', 45, 'San Roque Parish Hall', DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW());
INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, created_at)
VALUES (@l2, @group, 15.00, 'ACCEPTED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 4 HOUR));
INSERT INTO photo (file_path, offer_id) VALUES ('seed/event.png', LAST_INSERT_ID());

-- L3: Accepted and already released, so the group can confirm receipt
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Sliced bread', 'BAKED', 'AMBIENT_DRY', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), NOW());
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 10.00, 'ACCEPTED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 6 HOUR));
SET @l3 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/bread.jpg', @l3);
INSERT INTO use_detail (use_description, beneficiaries, headcount, place, due_at, attested_at)
VALUES ('School snack program', 'Grade school pupils', 80, 'Commonwealth Elementary School', DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW());
INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, released_at, created_at)
VALUES (@l3, @group, 10.00, 'ACCEPTED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 5 HOUR));
INSERT INTO photo (file_path, offer_id) VALUES ('seed/event.png', LAST_INSERT_ID());

-- L4: Rescued two days ago; the group's report is not in yet (due in five days: a banner on its dashboard)
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Assorted pastries', 'BAKED', 'AMBIENT_DRY', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 18.00, 'RESCUED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 4 DAY));
SET @l4 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/bread.jpg', @l4);
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, attested_at)
VALUES ('Youth center merienda', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'Out-of-school youth', 35, 'Barangay Holy Spirit Youth Center', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));
INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, released_at, received_at, created_at)
VALUES (@l4, @group, 18.00, 'COMPLETED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 49 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 4 DAY));
SET @o4 = LAST_INSERT_ID();
INSERT INTO photo (file_path, offer_id) VALUES ('seed/event.png', @o4);
INSERT INTO status_change (entity_type, entity_id, from_status, to_status, changed_by, changed_at) VALUES
 ('OFFER', @o4, 'ACCEPTED', 'COMPLETED', @group, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
 ('POST',  @l4, 'ACCEPTED', 'RESCUED',   @group, DATE_SUB(NOW(), INTERVAL 48 HOUR));

-- L5: Rescued five days ago with its report submitted (shows on the handoff record)
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Bottled juice', 'PACKAGED', 'AMBIENT_DRY', DATE_ADD(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY));
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 12.00, 'RESCUED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 7 DAY));
SET @l5 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/vegetables.png', @l5);
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, attested_at)
VALUES ('Fun run hydration', DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'Fun run participants', 120, 'Quezon Memorial Circle', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY));
INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, released_at, received_at, created_at)
VALUES (@l5, @group, 12.00, 'COMPLETED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 121 HOUR), DATE_SUB(NOW(), INTERVAL 120 HOUR), DATE_SUB(NOW(), INTERVAL 7 DAY));
SET @o5 = LAST_INSERT_ID();
INSERT INTO photo (file_path, offer_id) VALUES ('seed/event.png', @o5);
INSERT INTO report (offer_id, event_name, event_date, people_served, description, photo_consent_at, submitted_at)
VALUES (@o5, 'Fun run hydration', DATE_SUB(CURDATE(), INTERVAL 5 DAY), 118, 'All 12 kg of juice was handed out at the water stations.', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));
INSERT INTO photo (file_path, report_id) VALUES ('seed/event.png', LAST_INSERT_ID());
INSERT INTO status_change (entity_type, entity_id, from_status, to_status, changed_by, changed_at) VALUES
 ('OFFER', @o5, 'ACCEPTED', 'COMPLETED', @group, DATE_SUB(NOW(), INTERVAL 120 HOUR)),
 ('POST',  @l5, 'ACCEPTED', 'RESCUED',   @group, DATE_SUB(NOW(), INTERVAL 120 HOUR));

-- L6: Expired by a missed pickup (the offer lapsed)
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Day-old pandesal', 'BAKED', 'AMBIENT_DRY', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY));
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 9.00, 'EXPIRED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 6 DAY));
SET @l6 = LAST_INSERT_ID();
INSERT INTO use_detail (use_description, beneficiaries, headcount, place, due_at, attested_at)
VALUES ('Shelter breakfast', 'Shelter residents', 25, 'Bahay Pag-asa, Quezon City', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY));
INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, created_at)
VALUES (@l6, @group, 9.00, 'LAPSED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 6 DAY));
SET @o6 = LAST_INSERT_ID();
INSERT INTO status_change (entity_type, entity_id, from_status, to_status, reason, changed_by, changed_at) VALUES
 ('OFFER', @o6, 'ACCEPTED', 'LAPSED',  'Pickup missed', NULL, DATE_SUB(NOW(), INTERVAL 70 HOUR)),
 ('POST',  @l6, 'ACCEPTED', 'EXPIRED', 'Pickup missed', NULL, DATE_SUB(NOW(), INTERVAL 70 HOUR));

-- L7: Cancelled by the donor
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Canned goods', 'PACKAGED', 'AMBIENT_DRY', DATE_ADD(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));
INSERT INTO post (kind, posted_by, city_id, kg, status, cancel_reason, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 25.00, 'CANCELLED', 'Another charity collected it', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 8 DAY));
SET @l7 = LAST_INSERT_ID();
INSERT INTO status_change (entity_type, entity_id, from_status, to_status, reason, changed_by, changed_at)
VALUES ('POST', @l7, 'OPEN', 'CANCELLED', 'Another charity collected it', @business, DATE_SUB(NOW(), INTERVAL 7 DAY));

-- ROUTE 2 -------------------------------------------------------------------------------------------------
-- R1: an Open request with two offers to choose from (a Business donor and a Casual donor)
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, message, accepted_categories, attested_at)
VALUES ('School feeding program', DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'Grade school pupils', 150, 'Commonwealth Elementary School, Quezon City',
        DATE_ADD(NOW(), INTERVAL 4 DAY), 'Rice and canned goods are the most useful.', 'RAW,PACKAGED,BAKED', NOW());
INSERT INTO post (kind, posted_by, city_id, kg, status, use_detail_id, created_at)
VALUES ('REQUEST', @group, @qc, 40.00, 'OPEN', LAST_INSERT_ID(), NOW());
SET @r1 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/venue.jpg', @r1);
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, handover_instructions, attested_at)
VALUES ('Sacks of rice', 'RAW', 'AMBIENT_DRY', DATE_ADD(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 'Loading bay B, ask for Arthur.', NOW());
INSERT INTO offer (post_id, offerer_id, kg, status, food_detail_id, created_at)
VALUES (@r1, @business, 25.00, 'OFFERED', LAST_INSERT_ID(), NOW());
INSERT INTO photo (file_path, offer_id) VALUES ('seed/rice.png', LAST_INSERT_ID());
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Canned sardines', 'PACKAGED', 'AMBIENT_DRY', DATE_ADD(NOW(), INTERVAL 90 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), NOW());
INSERT INTO offer (post_id, offerer_id, kg, status, food_detail_id, created_at)
VALUES (@r1, @casual, 10.00, 'OFFERED', LAST_INSERT_ID(), NOW());
INSERT INTO photo (file_path, offer_id) VALUES ('seed/vegetables.png', LAST_INSERT_ID());

-- R2: Accepted and already released by the donor, so the group can confirm receipt
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, accepted_categories, attested_at)
VALUES ('Typhoon relief packs', CURDATE(), 'Flood-affected families', 70, 'Barangay Payatas covered court', DATE_ADD(NOW(), INTERVAL 1 DAY), 'RAW,PACKAGED', DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT INTO post (kind, posted_by, city_id, kg, status, use_detail_id, created_at)
VALUES ('REQUEST', @group, @qc, 30.00, 'ACCEPTED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 1 DAY));
SET @r2 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/venue.jpg', @r2);
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Instant noodles and canned goods', 'PACKAGED', 'AMBIENT_DRY', DATE_ADD(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT INTO offer (post_id, offerer_id, kg, status, food_detail_id, released_at, created_at)
VALUES (@r2, @business, 28.00, 'ACCEPTED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 20 HOUR));
INSERT INTO photo (file_path, offer_id) VALUES ('seed/rice.png', LAST_INSERT_ID());

-- R3: Rescued nine days ago and the group never sent its report: the first screen load flags it to the admin
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at, accepted_categories, attested_at)
VALUES ('Community pantry restock', DATE_SUB(CURDATE(), INTERVAL 9 DAY), 'Pantry visitors', 90, 'Kapwa Community Pantry', DATE_SUB(NOW(), INTERVAL 10 DAY), 'RAW,PACKAGED,BAKED', DATE_SUB(NOW(), INTERVAL 12 DAY));
INSERT INTO post (kind, posted_by, city_id, kg, status, use_detail_id, created_at)
VALUES ('REQUEST', @group, @qc, 22.00, 'RESCUED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 12 DAY));
SET @r3 = LAST_INSERT_ID();
INSERT INTO photo (file_path, post_id) VALUES ('seed/venue.jpg', @r3);
INSERT INTO food_detail (description, category, storage, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Fresh bread loaves', 'BAKED', 'AMBIENT_DRY', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY));
INSERT INTO offer (post_id, offerer_id, kg, status, food_detail_id, released_at, received_at, created_at)
VALUES (@r3, @business, 22.00, 'COMPLETED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 217 HOUR), DATE_SUB(NOW(), INTERVAL 216 HOUR), DATE_SUB(NOW(), INTERVAL 12 DAY));
SET @o_r3 = LAST_INSERT_ID();
INSERT INTO photo (file_path, offer_id) VALUES ('seed/bread.jpg', @o_r3);
INSERT INTO status_change (entity_type, entity_id, from_status, to_status, changed_by, changed_at) VALUES
 ('OFFER', @o_r3, 'ACCEPTED', 'COMPLETED', @group, DATE_SUB(NOW(), INTERVAL 216 HOUR)),
 ('POST',  @r3,   'ACCEPTED', 'RESCUED',   @group, DATE_SUB(NOW(), INTERVAL 216 HOUR));
