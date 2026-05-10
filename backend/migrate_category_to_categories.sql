-- Ako veke ja imash starata kolona category (VARCHAR), izvrshi go ovoj skript ednash:
USE mesto;

ALTER TABLE companies ADD COLUMN categories JSON NULL;

UPDATE companies
SET categories = JSON_ARRAY(category)
WHERE categories IS NULL AND category IS NOT NULL;

UPDATE companies SET categories = JSON_ARRAY() WHERE categories IS NULL;

ALTER TABLE companies MODIFY categories JSON NOT NULL;

ALTER TABLE companies DROP COLUMN category;
