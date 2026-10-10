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
-- 1 approved donor, 1 approved food group, 1 pending donor (for the review screen)
INSERT INTO registration (role, org_name, org_type, city_id, contact_person, contact_phone,
                          email, status, reviewed_by, reviewed_at, submitted_at) VALUES
 ('DONOR',      'Sunrise Bakery',        'bakery', @qc, 'Maria Santos', '09171234567',
  'donor@aniplato.test',  'APPROVED', @admin, NOW(), NOW()),
 ('FOOD_GROUP', 'Kapwa Community Kitchen','charity', @qc, 'Jose Reyes',  '09181234567',
  'group@aniplato.test',  'APPROVED', @admin, NOW(), NOW());
SET @reg_donor = (SELECT registration_id FROM registration WHERE email = 'donor@aniplato.test');
SET @reg_group = (SELECT registration_id FROM registration WHERE email = 'group@aniplato.test');

INSERT INTO registration (role, org_name, org_type, city_id, contact_person, contact_phone,
                          email, status, submitted_at)
VALUES ('DONOR', 'Bahay Grocery', 'grocery', @mk, 'Ana Cruz', '09191234567',
        'pending@aniplato.test', 'PENDING', NOW());
SET @reg_pending = LAST_INSERT_ID();

-- Accounts for the approved registrations -----------------------------
INSERT INTO account (email, password_hash, role, status, must_change_password,
                     password_changed_at, registration_id, created_at) VALUES
 ('donor@aniplato.test', @pw, 'DONOR',      'ACTIVE', FALSE, NOW(), @reg_donor, NOW()),
 ('group@aniplato.test', @pw, 'FOOD_GROUP', 'ACTIVE', FALSE, NOW(), @reg_group, NOW());
SET @donor = (SELECT account_id FROM account WHERE email = 'donor@aniplato.test');

-- Documents: placeholder paths (no real files under uploads/) ----------
INSERT INTO document (registration_id, doc_type, file_path, expires_on, review, uploaded_at,
                      reviewed_by, reviewed_at) VALUES
 (@reg_donor,   'dti',             'seed/dti.pdf',      NULL,                              'OK', NOW(), @admin, NOW()),
 (@reg_donor,   'mayors_permit',   'seed/permit.pdf',   DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW(), @admin, NOW()),
 (@reg_donor,   'sanitary_permit', 'seed/sanitary.pdf', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW(), @admin, NOW()),
 (@reg_group,   'dswd',            'seed/dswd.pdf',     DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'OK', NOW(), @admin, NOW()),
 (@reg_group,   'authorization_letter', 'seed/auth.pdf', NULL,                             'OK', NOW(), @admin, NOW()),
 (@reg_pending, 'dti',             'seed/dti.pdf',      NULL,                              NULL, NOW(), NULL,   NULL),
 (@reg_pending, 'mayors_permit',   'seed/permit.pdf',   DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NULL, NOW(), NULL,  NULL),
 (@reg_pending, 'sanitary_permit', 'seed/sanitary.pdf', DATE_ADD(CURDATE(), INTERVAL 1 YEAR), NULL, NOW(), NULL,  NULL);

-- One Open listing so the food group has something to browse -----------
INSERT INTO listing (posted_by, city_id, food_description, category, storage, allergens, kg,
                     use_by, pickup_start, pickup_end, donor_attested_at, created_at)
VALUES (@donor, @qc, 'Day-old pandesal and loaf bread', 'BAKED', 'AMBIENT_DRY', 'gluten,eggs', 12.50,
        DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
        DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), NOW());
