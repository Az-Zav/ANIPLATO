-- Extra demo data for the admin screens. ADDITIVE: run once, after schema.sql and seed.sql.
-- It only inserts (nothing is dropped or changed). Every account has the password Aniplato123! (dev only).
USE aniplato;

SET @pw = '$2a$10$GA02ZoTIXq3qRsE2fg7rm.u3FQcJ1iWK2FiXQomukRmdxsB5kjSh.';
SET @admin    = (SELECT account_id FROM account WHERE email = 'admin@aniplato.test');
SET @business = (SELECT account_id FROM account WHERE email = 'business@aniplato.test');
SET @group    = (SELECT account_id FROM account WHERE email = 'group@aniplato.test');
SET @qc = (SELECT city_id FROM city WHERE name = 'Quezon City');
SET @md = (SELECT city_id FROM city WHERE name = 'Mandaluyong');
SET @pg = (SELECT city_id FROM city WHERE name = 'Pasig');

-- Pending: a Business donor and a Food group (the food group also has an earlier rejected registration) ----
INSERT INTO registration (role, name, city_id, contact_person, contact_phone, email, status,
                          rejection_reason, reviewed_by, reviewed_at, submitted_at) VALUES
 ('FOOD_GROUP', 'San Antonio Community Pantry', @pg, 'Elena R. Santos', '09182345671',
  'sanantonio@aniplato.test', 'REJECTED', 'The authorization letter did not name the contact person.',
  @admin, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));

INSERT INTO registration (role, name, city_id, contact_person, contact_phone, email, status, submitted_at) VALUES
 ('BUSINESS_DONOR', 'Manila Artisanal Bakeshop', @md, 'Maria Santos', '09171234568',
  'bakeshop@aniplato.test', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY));
SET @reg_bake = LAST_INSERT_ID();
INSERT INTO registration (role, name, city_id, contact_person, contact_phone, email, status, submitted_at) VALUES
 ('FOOD_GROUP', 'San Antonio Community Pantry', @pg, 'Elena R. Santos', '09182345671',
  'sanantonio@aniplato.test', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY));
SET @reg_pantry = LAST_INSERT_ID();

INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at) VALUES
 (@reg_bake,   'mayors_permit',        'seed/permit.pdf',   DATE_ADD(CURDATE(), INTERVAL 11 MONTH), NULL, NOW()),
 (@reg_bake,   'sanitary_permit',      'seed/sanitary.pdf', DATE_ADD(CURDATE(), INTERVAL 8 MONTH),  NULL, NOW()),
 (@reg_pantry, 'dswd',                 'seed/dswd.pdf',     DATE_ADD(CURDATE(), INTERVAL 8 MONTH),  NULL, NOW()),
 (@reg_pantry, 'authorization_letter', 'seed/auth.pdf',     NULL,                                   NULL, NOW());

-- A suspended Casual donor whose certificate expires soon ----------------------
INSERT INTO registration (role, name, city_id, contact_person, contact_phone, email, status,
                          reviewed_by, reviewed_at, submitted_at) VALUES
 ('CASUAL_DONOR', 'Dela Cruz Household', @qc, 'Rosa Dela Cruz', '09191112222',
  'delacruz@aniplato.test', 'APPROVED', @admin, NOW(), DATE_SUB(NOW(), INTERVAL 30 DAY));
SET @reg_dc = LAST_INSERT_ID();
INSERT INTO account (email, password_hash, role, status, suspend_reason, must_change_password,
                     password_changed_at, registration_id, created_at)
VALUES ('delacruz@aniplato.test', @pw, 'CASUAL_DONOR', 'SUSPENDED', 'Admin suspended after a safety notice',
        FALSE, NOW(), @reg_dc, DATE_SUB(NOW(), INTERVAL 29 DAY));
INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at, reviewed_by, reviewed_at)
VALUES (@reg_dc, 'barangay_certificate', 'seed/barangay.pdf', DATE_ADD(CURDATE(), INTERVAL 12 DAY), 'OK',
        DATE_SUB(NOW(), INTERVAL 29 DAY), @admin, DATE_SUB(NOW(), INTERVAL 29 DAY));

-- Numbers for the dashboard and reports: one rescued listing (with a completed offer and a report),
-- one expired and one cancelled listing, and an open safety notice -----------------------------------
INSERT INTO food_detail (description, category, storage, allergens, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Cooked pancit canton trays', 'COOKED', 'HOT_HOLD', NULL,
        DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), NOW());
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 20.00, 'RESCUED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 5 DAY));
SET @post_rescued = LAST_INSERT_ID();

INSERT INTO food_detail (description, category, storage, allergens, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Assorted bread', 'BAKED', 'AMBIENT_DRY', NULL,
        DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), NOW());
INSERT INTO post (kind, posted_by, city_id, kg, status, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 8.00, 'EXPIRED', LAST_INSERT_ID(), DATE_SUB(NOW(), INTERVAL 6 DAY));

INSERT INTO food_detail (description, category, storage, allergens, use_by, pickup_start, pickup_end, attested_at)
VALUES ('Fresh vegetables', 'RAW', 'CHILLED', NULL,
        DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), NOW());
INSERT INTO post (kind, posted_by, city_id, kg, status, cancel_reason, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 5.00, 'CANCELLED', 'Donor no longer has the food', LAST_INSERT_ID(),
        DATE_SUB(NOW(), INTERVAL 4 DAY));

INSERT INTO use_detail (use_description, beneficiaries, headcount, place, due_at, attested_at)
VALUES ('Weekend feeding program', 'Street children and elderly', 60, 'Kapwa Community Kitchen, Quezon City',
        DATE_SUB(NOW(), INTERVAL 4 DAY), NOW());
INSERT INTO offer (post_id, offerer_id, kg, status, use_detail_id, released_at, received_at, created_at)
VALUES (@post_rescued, @group, 20.00, 'COMPLETED', LAST_INSERT_ID(),
        DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));
SET @offer_done = LAST_INSERT_ID();

INSERT INTO report (offer_id, event_name, event_date, people_served, description, photo_consent_at, submitted_at)
VALUES (@offer_done, 'Weekend feeding program', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 60,
        'Served pancit canton to 60 people.', NOW(), DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO safety_notice (offer_id, kind, raised_by, concern, status, raised_at)
VALUES (@offer_done, 'CONCERN', @group, 'Warm, sour smell at pickup', 'OPEN', DATE_SUB(NOW(), INTERVAL 3 DAY));
