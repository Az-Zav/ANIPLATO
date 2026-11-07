-- Developers and tests only: wipes the aniplato database and creates it empty. The app's setup screen never runs this.
-- Order: reset.sql, schema.sql, then seed.sql (setup-db.ps1 and the tests do all of it).
DROP DATABASE IF EXISTS aniplato;
CREATE DATABASE aniplato CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aniplato;
