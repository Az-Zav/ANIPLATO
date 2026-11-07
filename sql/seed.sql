-- ANIPLATO starting data: one admin account and nothing else. Run after schema.sql (setup-db.ps1 does both).
-- Sign in as admin@aniplato.test with the password Aniplato123!; the app makes you choose a new one at once.
-- The app's own setup screen does not use this file: it creates the admin you type in.
USE aniplato;

INSERT INTO account (email, password_hash, role, status, must_change_password,
                     password_changed_at, admin_name, created_at)
VALUES ('admin@aniplato.test', '$2a$10$GA02ZoTIXq3qRsE2fg7rm.u3FQcJ1iWK2FiXQomukRmdxsB5kjSh.', 'ADMIN', 'ACTIVE', TRUE,
        NOW(), 'ANIPLATO Admin', NOW());
