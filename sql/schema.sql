-- ANIPLATO schema (MySQL 8). Ten tables. Run once on an empty database.
DROP DATABASE IF EXISTS aniplato;
CREATE DATABASE aniplato CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aniplato;

-- 1. city -------------------------------------------------------------
CREATE TABLE city (
  city_id INT AUTO_INCREMENT PRIMARY KEY,
  name    VARCHAR(60) NOT NULL UNIQUE
);

-- 3. account (login only). Its link to registration is added at the end,
--    because registration also points back to account (reviewed_by).
CREATE TABLE account (
  account_id           INT AUTO_INCREMENT PRIMARY KEY,
  email                VARCHAR(120) NOT NULL UNIQUE,
  password_hash        VARCHAR(60)  NOT NULL,
  role                 ENUM('ADMIN','DONOR','FOOD_GROUP') NOT NULL,
  status               ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  suspend_reason       VARCHAR(500) NULL,
  must_change_password BOOLEAN  NOT NULL DEFAULT TRUE,
  password_changed_at  DATETIME NOT NULL,
  registration_id      INT NULL UNIQUE,
  admin_name           VARCHAR(120) NULL,
  created_at           DATETIME NOT NULL,
  -- admin has no registration; donors and food groups always have one
  CONSTRAINT chk_account_registration CHECK (
    (role = 'ADMIN' AND registration_id IS NULL) OR
    (role <> 'ADMIN' AND registration_id IS NOT NULL))
);

-- 2. registration ------------------------------------------------------
CREATE TABLE registration (
  registration_id  INT AUTO_INCREMENT PRIMARY KEY,
  role             ENUM('DONOR','FOOD_GROUP') NOT NULL,
  org_name         VARCHAR(120) NOT NULL,
  org_type         ENUM('restaurant','bakery','canteen','caterer','grocery',
                        'parish','school','orphanage','charity') NOT NULL,
  city_id          INT NOT NULL,
  contact_person   VARCHAR(100) NOT NULL,
  contact_phone    VARCHAR(20)  NOT NULL,
  email            VARCHAR(120) NOT NULL,          -- not unique: rejected orgs may register again
  status           ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  rejection_reason VARCHAR(500) NULL,
  reviewed_by      INT NULL,
  reviewed_at      DATETIME NULL,
  submitted_at     DATETIME NOT NULL,
  FOREIGN KEY (city_id)     REFERENCES city(city_id),
  FOREIGN KEY (reviewed_by) REFERENCES account(account_id),
  INDEX idx_registration_status (status, submitted_at),
  INDEX idx_registration_email (email)
);

ALTER TABLE account
  ADD CONSTRAINT fk_account_registration
  FOREIGN KEY (registration_id) REFERENCES registration(registration_id);

-- 4. document ----------------------------------------------------------
CREATE TABLE document (
  document_id     INT AUTO_INCREMENT PRIMARY KEY,
  registration_id INT NOT NULL,
  doc_type        ENUM('dti','sec','cda','mayors_permit','sanitary_permit','fda_lto',
                       'dswd','pcnc','sec_non_stock','deped_ched_tesda',
                       'diocese_endorsement','authorization_letter') NOT NULL,
  file_path       VARCHAR(255) NOT NULL,
  expires_on      DATE NULL,                        -- NULL = no expiry
  review          ENUM('OK','FLAGGED') NULL,        -- NULL = awaiting review
  flag_reason     VARCHAR(500) NULL,
  uploaded_at     DATETIME NOT NULL,
  reviewed_by     INT NULL,
  reviewed_at     DATETIME NULL,
  FOREIGN KEY (registration_id) REFERENCES registration(registration_id),
  FOREIGN KEY (reviewed_by)     REFERENCES account(account_id),
  INDEX idx_document_registration (registration_id, doc_type)
);

-- 5. listing -----------------------------------------------------------
CREATE TABLE listing (
  listing_id            INT AUTO_INCREMENT PRIMARY KEY,
  posted_by             INT NOT NULL,
  city_id               INT NOT NULL,
  food_description      VARCHAR(200) NOT NULL,
  category              ENUM('RAW','COOKED','BAKED','PACKAGED') NOT NULL,
  storage               ENUM('AMBIENT_DRY','CHILLED','HOT_HOLD') NOT NULL,
  allergens             SET('gluten','dairy','nuts','eggs','halal') NULL,
  kg                    DECIMAL(6,2) NOT NULL,
  use_by                DATETIME NOT NULL,
  pickup_start          DATETIME NOT NULL,
  pickup_end            DATETIME NOT NULL,
  handover_instructions VARCHAR(500) NULL,
  donor_attested_at     DATETIME NOT NULL,
  status                ENUM('OPEN','ACCEPTED','RESCUED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'OPEN',
  cancel_reason         VARCHAR(500) NULL,
  created_at            DATETIME NOT NULL,
  FOREIGN KEY (posted_by) REFERENCES account(account_id),
  FOREIGN KEY (city_id)   REFERENCES city(city_id),
  CONSTRAINT chk_listing_kg      CHECK (kg > 0),
  CONSTRAINT chk_listing_window  CHECK (pickup_start < pickup_end),
  CONSTRAINT chk_listing_use_by  CHECK (pickup_end < use_by),
  INDEX idx_listing_board (status, city_id),
  INDEX idx_listing_poster (posted_by)
);

-- 6. offer -------------------------------------------------------------
CREATE TABLE offer (
  offer_id          INT AUTO_INCREMENT PRIMARY KEY,   -- also the handoff record number
  listing_id        INT NOT NULL,
  group_id          INT NOT NULL,
  use_description   VARCHAR(300) NOT NULL,
  beneficiaries     VARCHAR(200) NOT NULL,
  headcount         INT NOT NULL,
  place             VARCHAR(200) NOT NULL,
  pickup_time       DATETIME NOT NULL,
  message           VARCHAR(500) NULL,
  group_attested_at DATETIME NOT NULL,
  status            ENUM('OFFERED','ACCEPTED','COMPLETED','DECLINED','WITHDRAWN',
                         'EXPIRED','LAPSED','CANCELLED') NOT NULL DEFAULT 'OFFERED',
  released_at       DATETIME NULL,
  received_at       DATETIME NULL,
  created_at        DATETIME NOT NULL,
  FOREIGN KEY (listing_id) REFERENCES listing(listing_id),
  FOREIGN KEY (group_id)   REFERENCES account(account_id),
  CONSTRAINT uq_offer_one_per_group UNIQUE (listing_id, group_id),   -- R3.1
  CONSTRAINT chk_offer_headcount CHECK (headcount > 0),
  INDEX idx_offer_group (group_id, status)
);

-- 7. offer_photo -------------------------------------------------------
CREATE TABLE offer_photo (
  photo_id  INT AUTO_INCREMENT PRIMARY KEY,
  offer_id  INT NOT NULL,
  file_path VARCHAR(255) NOT NULL,
  FOREIGN KEY (offer_id) REFERENCES offer(offer_id) ON DELETE CASCADE
);

-- 8. safety_notice -----------------------------------------------------
CREATE TABLE safety_notice (
  notice_id   INT AUTO_INCREMENT PRIMARY KEY,
  offer_id    INT NOT NULL,
  raised_by   INT NOT NULL,
  concern     VARCHAR(500) NOT NULL,
  status      ENUM('OPEN','DISMISSED','ACCOUNT_SUSPENDED') NOT NULL DEFAULT 'OPEN',
  admin_note  VARCHAR(500) NULL,
  resolved_by INT NULL,
  resolved_at DATETIME NULL,
  raised_at   DATETIME NOT NULL,
  FOREIGN KEY (offer_id)    REFERENCES offer(offer_id),
  FOREIGN KEY (raised_by)   REFERENCES account(account_id),
  FOREIGN KEY (resolved_by) REFERENCES account(account_id),
  INDEX idx_notice_status (status, raised_at)
);

-- 9. status_change (append-only log) ----------------------------------
CREATE TABLE status_change (
  change_id   INT AUTO_INCREMENT PRIMARY KEY,
  entity_type ENUM('REGISTRATION','ACCOUNT','DOCUMENT','LISTING','OFFER','NOTICE') NOT NULL,
  entity_id   INT NOT NULL,
  from_status VARCHAR(30) NULL,                     -- NULL for a new record
  to_status   VARCHAR(30) NOT NULL,
  reason      VARCHAR(500) NULL,
  changed_by  INT NULL,                             -- NULL = the system (maintenance)
  changed_at  DATETIME NOT NULL,
  FOREIGN KEY (changed_by) REFERENCES account(account_id),
  INDEX idx_status_change_entity (entity_type, entity_id, changed_at)
);

-- 10. email_log --------------------------------------------------------
CREATE TABLE email_log (
  email_id INT AUTO_INCREMENT PRIMARY KEY,
  to_email VARCHAR(120) NOT NULL,
  kind     ENUM('RECEIVED','APPROVED','NOT_APPROVED') NOT NULL,
  sent_at  DATETIME NOT NULL,
  success  BOOLEAN NOT NULL,
  error    VARCHAR(300) NULL
);

-- Lookup data: Metro Manila ---------------------------------------------
INSERT INTO city (name) VALUES
 ('Caloocan'),('Las Piñas'),('Makati'),('Malabon'),('Mandaluyong'),('Manila'),
 ('Marikina'),('Muntinlupa'),('Navotas'),('Parañaque'),('Pasay'),('Pasig'),
 ('Quezon City'),('San Juan'),('Taguig'),('Valenzuela'),('Pateros');
