-- Adds use_detail.food_needed (requests: the food asked for) to a database made before it existed.
-- Run ONCE on an existing database (schema.sql already has the column, so a fresh database needs nothing).
-- Old requests get their event name as the food needed, so nothing is blank; the people who posted them can
-- not edit it, which is fine: a request is cancelled and posted again, never edited.
USE aniplato;

ALTER TABLE use_detail
  ADD COLUMN food_needed VARCHAR(200) NULL AFTER use_description;

UPDATE use_detail ud
  JOIN post p ON p.use_detail_id = ud.use_detail_id
   SET ud.food_needed = ud.use_description
 WHERE ud.food_needed IS NULL;
