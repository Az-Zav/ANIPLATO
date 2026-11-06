-- Adds the table that holds uploaded photos and documents. Safe to run twice; changes no existing row.
-- Run as the database owner (the app user has no CREATE right):  scripts/migrate-online-db.ps1
USE aniplato;

-- 14. stored_file: the bytes of every uploaded photo and document, so all users see the same files.
-- document.file_path and photo.file_path hold the key; there is no foreign key because the file is saved before its row.
CREATE TABLE IF NOT EXISTS stored_file (
  file_path  VARCHAR(255) PRIMARY KEY,
  content    MEDIUMBLOB NOT NULL,
  size_bytes INT NOT NULL,
  stored_at  DATETIME NOT NULL
);

