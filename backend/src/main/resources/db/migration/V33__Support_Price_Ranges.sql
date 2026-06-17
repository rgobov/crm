-- Support price ranges (fixed price vs range) in services table
ALTER TABLE services RENAME COLUMN price TO price_min;
ALTER TABLE services ADD COLUMN price_max INTEGER;
