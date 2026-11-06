-- Puts the letter n-with-tilde back in two city names that an earlier online load stored as "?" (the load script sent the
-- text as ASCII). Safe to run twice and on a good database: it only touches a name that still has the "?".
-- The letter is written as its UTF-8 bytes so this file itself cannot be damaged the same way.
USE aniplato;
UPDATE city SET name = CONCAT('Las Pi', _utf8mb4 X'C3B1', 'as') WHERE name = 'Las Pi?as';
UPDATE city SET name = CONCAT('Para', _utf8mb4 X'C3B1', 'aque') WHERE name = 'Para?aque';
