-- ANIPLATO schema (MySQL 8.0.16+ for CHECK constraints). Thirteen tables. Run once on an empty database.
-- Source: Project Spec section 7.6. Change this file, the spec's 7.6 and the ER diagram in docs/ARCHITECTURE.md together.
DROP DATABASE IF EXISTS aniplato;
CREATE DATABASE aniplato CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aniplato;

-- 1. city -------------------------------------------------------------
CREATE TABLE city (
  city_id INT AUTO_INCREMENT PRIMARY KEY,
  name    VARCHAR(60) NOT NULL UNIQUE
);

-- 2. account (login only). Its link to registration is added after registration
--    exists, because registration also points back to account (reviewed_by).
CREATE TABLE account (
  account_id           INT AUTO_INCREMENT PRIMARY KEY,
  email                VARCHAR(120) NOT NULL UNIQUE,
  password_hash        VARCHAR(60)  NOT NULL,
  role                 ENUM('ADMIN','BUSINESS_DONOR','CASUAL_DONOR','FOOD_GROUP') NOT NULL,
  status               ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  suspend_reason       VARCHAR(500) NULL,
  must_change_password BOOLEAN  NOT NULL DEFAULT TRUE,
  password_changed_at  DATETIME NOT NULL,
  registration_id      INT NULL UNIQUE,
  admin_name           VARCHAR(120) NULL,
  created_at           DATETIME NOT NULL,
  -- admin has no registration; every other role always has one
  CONSTRAINT chk_account_registration CHECK (
    (role = 'ADMIN' AND registration_id IS NULL) OR
    (role <> 'ADMIN' AND registration_id IS NOT NULL))
);

-- 3. registration ------------------------------------------------------
CREATE TABLE registration (
  registration_id  INT AUTO_INCREMENT PRIMARY KEY,
  role             ENUM('BUSINESS_DONOR','CASUAL_DONOR','FOOD_GROUP') NOT NULL,
  name             VARCHAR(120) NOT NULL,          -- organization, person or household
  city_id          INT NOT NULL,
  contact_person   VARCHAR(100) NOT NULL,
  contact_phone    VARCHAR(20)  NOT NULL,
  email            VARCHAR(120) NOT NULL,          -- not unique: rejected applicants may register again
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
-- A renewal is a new row; the document in force is the latest one with review OK per type.
CREATE TABLE document (
  document_id     INT AUTO_INCREMENT PRIMARY KEY,
  registration_id INT NOT NULL,
  doc_type        ENUM('mayors_permit','sanitary_permit','barangay_certificate','dswd','pcnc',
                       'sec_non_stock','deped_ched_tesda','diocese_endorsement',
                       'authorization_letter') NOT NULL,
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

-- 5. food_detail: what the donor gives (a listing, or a Route 2 offer) --------
CREATE TABLE food_detail (
  food_detail_id        INT AUTO_INCREMENT PRIMARY KEY,
  description           VARCHAR(200) NOT NULL,
  category              ENUM('RAW','COOKED','BAKED','PACKAGED') NOT NULL,
  storage               ENUM('AMBIENT_DRY','CHILLED','HOT_HOLD') NOT NULL,
  allergens             SET('gluten','dairy','nuts','eggs','halal') NULL,
  use_by                DATETIME NOT NULL,
  pickup_start          DATETIME NOT NULL,
  pickup_end            DATETIME NOT NULL,
  handover_instructions VARCHAR(500) NULL,         -- hidden until the offer is accepted
  attested_at           DATETIME NOT NULL,         -- donor attestation
  CONSTRAINT chk_food_window CHECK (pickup_start < pickup_end),
  CONSTRAINT chk_food_use_by CHECK (pickup_end < use_by)
);

-- 6. use_detail: what the food group needs or will do (a request, or a Route 1 offer)
CREATE TABLE use_detail (
  use_detail_id       INT AUTO_INCREMENT PRIMARY KEY,
  use_description     VARCHAR(300) NOT NULL,        -- event, program or use
  event_date          DATE NULL,                    -- required on a request (service rule)
  beneficiaries       VARCHAR(200) NOT NULL,
  headcount           INT NOT NULL,
  place               VARCHAR(200) NOT NULL,
  due_at              DATETIME NOT NULL,            -- needed-by (request) or pickup time (offer)
  message             VARCHAR(500) NULL,
  accepted_categories SET('RAW','COOKED','BAKED','PACKAGED') NULL,   -- requests only
  attested_at         DATETIME NOT NULL,            -- group attestation
  CONSTRAINT chk_use_headcount CHECK (headcount > 0)
);

-- 7. post: one table for listings (Route 1) and requests (Route 2) -----------
CREATE TABLE post (
  post_id        INT AUTO_INCREMENT PRIMARY KEY,
  kind           ENUM('LISTING','REQUEST') NOT NULL,
  posted_by      INT NOT NULL,
  city_id        INT NOT NULL,                     -- copied from the poster's registration
  kg             DECIMAL(6,2) NOT NULL,
  status         ENUM('OPEN','ACCEPTED','RESCUED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'OPEN',
  cancel_reason  VARCHAR(500) NULL,
  food_detail_id INT NULL UNIQUE,                  -- listing only
  use_detail_id  INT NULL UNIQUE,                  -- request only
  created_at     DATETIME NOT NULL,
  FOREIGN KEY (posted_by)      REFERENCES account(account_id),
  FOREIGN KEY (city_id)        REFERENCES city(city_id),
  FOREIGN KEY (food_detail_id) REFERENCES food_detail(food_detail_id),
  FOREIGN KEY (use_detail_id)  REFERENCES use_detail(use_detail_id),
  CONSTRAINT chk_post_kg CHECK (kg > 0),
  CONSTRAINT chk_post_detail CHECK (
    (kind = 'LISTING' AND food_detail_id IS NOT NULL AND use_detail_id IS NULL) OR
    (kind = 'REQUEST' AND use_detail_id  IS NOT NULL AND food_detail_id IS NULL)),
  INDEX idx_post_board (kind, status, city_id),
  INDEX idx_post_poster (posted_by)
);

-- 8. offer: one table for both routes ---------------------------------------
-- offer_id is also the handoff record number (HR-year-id, worked out, not stored).
CREATE TABLE offer (
  offer_id       INT AUTO_INCREMENT PRIMARY KEY,
  post_id        INT NOT NULL,
  offerer_id     INT NOT NULL,
  kg             DECIMAL(6,2) NOT NULL,
  status         ENUM('OFFERED','ACCEPTED','COMPLETED','DECLINED','WITHDRAWN',
                      'EXPIRED','LAPSED','CANCELLED') NOT NULL DEFAULT 'OFFERED',
  food_detail_id INT NULL UNIQUE,                  -- Route 2 only (offer on a request)
  use_detail_id  INT NULL UNIQUE,                  -- Route 1 only (offer on a listing)
  released_at    DATETIME NULL,
  received_at    DATETIME NULL,
  created_at     DATETIME NOT NULL,
  FOREIGN KEY (post_id)        REFERENCES post(post_id),
  FOREIGN KEY (offerer_id)     REFERENCES account(account_id),
  FOREIGN KEY (food_detail_id) REFERENCES food_detail(food_detail_id),
  FOREIGN KEY (use_detail_id)  REFERENCES use_detail(use_detail_id),
  CONSTRAINT uq_offer_one_per_offerer UNIQUE (post_id, offerer_id),   -- R3.1
  CONSTRAINT chk_offer_kg CHECK (kg > 0),
  -- exactly one detail link; the service makes it the opposite of the post's (A9 stores facts only)
  CONSTRAINT chk_offer_detail CHECK ((food_detail_id IS NULL) <> (use_detail_id IS NULL)),
  INDEX idx_offer_offerer (offerer_id, status)
);

-- 9. report: the food group's documentation of one completed handoff -------
CREATE TABLE report (
  report_id        INT AUTO_INCREMENT PRIMARY KEY,
  offer_id         INT NOT NULL UNIQUE,
  event_name       VARCHAR(150) NOT NULL,
  event_date       DATE NOT NULL,
  people_served    INT NOT NULL,
  description      VARCHAR(1000) NOT NULL,
  photo_consent_at DATETIME NOT NULL,
  submitted_at     DATETIME NOT NULL,
  FOREIGN KEY (offer_id) REFERENCES offer(offer_id),
  CONSTRAINT chk_report_served CHECK (people_served >= 0)
);

-- 10. photo: belongs to exactly one of post, offer or report -----------------
-- The 1-to-5 count per owner is a service rule.
CREATE TABLE photo (
  photo_id  INT AUTO_INCREMENT PRIMARY KEY,
  file_path VARCHAR(255) NOT NULL,
  post_id   INT NULL,
  offer_id  INT NULL,
  report_id INT NULL,
  FOREIGN KEY (post_id)   REFERENCES post(post_id),
  FOREIGN KEY (offer_id)  REFERENCES offer(offer_id),
  FOREIGN KEY (report_id) REFERENCES report(report_id),
  CONSTRAINT chk_photo_owner CHECK (
    (post_id IS NOT NULL) + (offer_id IS NOT NULL) + (report_id IS NOT NULL) = 1)
);

-- 11. safety_notice ----------------------------------------------------
CREATE TABLE safety_notice (
  notice_id   INT AUTO_INCREMENT PRIMARY KEY,
  offer_id    INT NOT NULL,
  kind        ENUM('CONCERN','OVERDUE_REPORT') NOT NULL,
  raised_by   INT NULL,                             -- NULL = the system
  concern     VARCHAR(500) NULL,                    -- NULL for an overdue report
  status      ENUM('OPEN','DISMISSED','ACCOUNT_SUSPENDED') NOT NULL DEFAULT 'OPEN',
  admin_note  VARCHAR(500) NULL,                    -- required on resolve (service rule)
  resolved_by INT NULL,
  resolved_at DATETIME NULL,
  raised_at   DATETIME NOT NULL,
  -- helper for "one OVERDUE_REPORT per offer": NULL for concerns, so many concerns are allowed
  overdue_key TINYINT AS (IF(kind = 'OVERDUE_REPORT', 1, NULL)) STORED,
  FOREIGN KEY (offer_id)    REFERENCES offer(offer_id),
  FOREIGN KEY (raised_by)   REFERENCES account(account_id),
  FOREIGN KEY (resolved_by) REFERENCES account(account_id),
  CONSTRAINT uq_notice_one_overdue UNIQUE (offer_id, overdue_key),
  CONSTRAINT chk_notice_kind CHECK (
    (kind = 'CONCERN'        AND raised_by IS NOT NULL AND concern IS NOT NULL) OR
    (kind = 'OVERDUE_REPORT' AND raised_by IS NULL     AND concern IS NULL)),
  INDEX idx_notice_status (status, raised_at)
);

-- 12. status_change (append-only log) ----------------------------------
CREATE TABLE status_change (
  change_id   INT AUTO_INCREMENT PRIMARY KEY,
  entity_type ENUM('REGISTRATION','ACCOUNT','DOCUMENT','POST','OFFER','NOTICE') NOT NULL,
  entity_id   INT NOT NULL,                         -- points at entity_type's table, no FK
  from_status VARCHAR(30) NULL,                     -- NULL for a new record
  to_status   VARCHAR(30) NOT NULL,
  reason      VARCHAR(500) NULL,
  changed_by  INT NULL,                             -- NULL = the system (maintenance)
  changed_at  DATETIME NOT NULL,
  FOREIGN KEY (changed_by) REFERENCES account(account_id),
  INDEX idx_status_change_entity (entity_type, entity_id, changed_at)
);

-- 13. email_log --------------------------------------------------------
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
