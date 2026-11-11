-- Local teaching account for storefront login, separate from MySQL credentials.
-- Username: forest    Password: Forest2026!
-- BCrypt cost 12; existing accounts and passwords are never overwritten.
SET NAMES utf8mb4;
USE fangcun_forest;

INSERT INTO store_user (username, password_hash, display_name, role, status)
SELECT 'forest', '$2b$12$7zr67HpHjYH9OX4eANvI2.YMq4lpfpGvuwaS2bHvcldZisfWVUwUe',
       '森林旅人', 'CUSTOMER', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM store_user WHERE username = 'forest');
