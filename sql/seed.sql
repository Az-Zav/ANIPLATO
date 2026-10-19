-- ANIPLATO demo data. Run after schema.sql (setup-db.ps1 does both).
-- Every account below has the password:  Aniplato123!   (dev only)
USE aniplato;

SET @pw = '$2a$10$GA02ZoTIXq3qRsE2fg7rm.u3FQcJ1iWK2FiXQomukRmdxsB5kjSh.';
SET @qc = (SELECT city_id FROM city WHERE name = 'Quezon City');
SET @mk = (SELECT city_id FROM city WHERE name = 'Makati');

-- Admin (no registration) ----------------------------------------------
INSERT INTO account (email, password_hash, role, status, must_change_password,
                     password_changed_at, admin_name, created_at)
VALUES ('admin@aniplato.test', @pw, 'ADMIN', 'ACTIVE', FALSE, NOW(), 'ANIPLATO Admin', NOW());
SET @admin = LAST_INSERT_ID();

-- Registrations --------------------------------------------------------
-- Approved: one Business donor, one Casual donor, one food group.
-- Pending: one Casual donor, for the review screen.
INSERT INTO registration (role, name, city_id, contact_person, contact_phone,
                          email, status, reviewed_by, reviewed_at, submitted_at) VALUES
 ('BUSINESS_DONOR', 'Sunrise Bakery',         @qc, 'Maria Santos', '09171234567',
  'business@aniplato.test', 'APPROVED', @admin, NOW(), NOW()),
 ('CASUAL_DONOR',   'Santos Household',       @qc, 'Elena Santos', '09201234567',
  'casual@aniplato.test',   'APPROVED', @admin, NOW(), NOW()),
 ('FOOD_GROUP',     'Kapwa Community Kitchen', @qc, 'Jose Reyes',  '09181234567',
  'group@aniplato.test',    'APPROVED', @admin, NOW(), NOW());
SET @reg_business = (SELECT registration_id FROM registration WHERE email = 'business@aniplato.test');
SET @reg_casual   = (SELECT registration_id FROM registration WHERE email = 'casual@aniplato.test');
SET @reg_group    = (SELECT registration_id FROM registration WHERE email = 'group@aniplato.test');

INSERT INTO registration (role, name, city_id, contact_person, contact_phone,
                          email, status, submitted_at)
VALUES ('CASUAL_DONOR', 'Ana Cruz', @mk, 'Ana Cruz', '09191234567',
        'pending@aniplato.test', 'PENDING', NOW());
SET @reg_pending = LAST_INSERT_ID();

-- Accounts for the approved registrations -----------------------------
INSERT INTO account (email, password_hash, role, status, must_change_password,
                     password_changed_at, registration_id, created_at) VALUES
 ('business@aniplato.test', @pw, 'BUSINESS_DONOR', 'ACTIVE', FALSE, NOW(), @reg_business, NOW()),
 ('casual@aniplato.test',   @pw, 'CASUAL_DONOR',   'ACTIVE', FALSE, NOW(), @reg_casual,   NOW()),
 ('group@aniplato.test',    @pw, 'FOOD_GROUP',     'ACTIVE', FALSE, NOW(), @reg_group,    NOW());
SET @business = (SELECT account_id FROM account WHERE email = 'business@aniplato.test');
SET @group    = (SELECT account_id FROM account WHERE email = 'group@aniplato.test');

-- Documents: placeholder paths (no real files under uploads/) ----------
INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at,
                      reviewed_by, reviewed_at) VALUES
 (@reg_business, 'mayors_permit',        'seed/permit.pdf',      DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW(), @admin, NOW()),
 (@reg_business, 'sanitary_permit',      'seed/sanitary.pdf',    DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW(), @admin, NOW()),
 (@reg_casual,   'barangay_certificate', 'seed/barangay.pdf',    DATE_ADD(CURDATE(), INTERVAL 6 MONTH), 'OK', NOW(), @admin, NOW()),
 (@reg_group,    'dswd',                 'seed/dswd.pdf',        DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW(), @admin, NOW()),
 (@reg_group,    'authorization_letter', 'seed/auth.pdf',        NULL,                                 'OK', NOW(), @admin, NOW()),
 (@reg_pending,  'barangay_certificate', 'seed/barangay.pdf',    DATE_ADD(CURDATE(), INTERVAL 6 MONTH), NULL, NOW(), NULL, NULL);

-- One Open listing (Route 1) so the food group has something to browse ---
INSERT INTO food_detail (description, category, storage, allergens, use_by, pickup_start,
                         pickup_end, attested_at)
VALUES ('Day-old pandesal and loaf bread', 'BAKED', 'AMBIENT_DRY', 'gluten,eggs',
        DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
        DATE_ADD(NOW(), INTERVAL 2 DAY), NOW());
INSERT INTO post (kind, posted_by, city_id, kg, food_detail_id, created_at)
VALUES ('LISTING', @business, @qc, 12.50, LAST_INSERT_ID(), NOW());
INSERT INTO photo (file_path, post_id) VALUES ('seed/bread.jpg', LAST_INSERT_ID());

-- One Open request (Route 2) so donors have something to browse -------------
INSERT INTO use_detail (use_description, event_date, beneficiaries, headcount, place, due_at,
                        accepted_categories, attested_at)
VALUES ('Weekend feeding program', DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'Street children and elderly', 80,
        'Kapwa Community Kitchen, Quezon City', DATE_ADD(NOW(), INTERVAL 5 DAY),
        'RAW,PACKAGED,BAKED', NOW());
INSERT INTO post (kind, posted_by, city_id, kg, use_detail_id, created_at)
VALUES ('REQUEST', @group, @qc, 30.00, LAST_INSERT_ID(), NOW());
INSERT INTO photo (file_path, post_id) VALUES ('seed/venue.jpg', LAST_INSERT_ID());
