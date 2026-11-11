-- Local teaching administrator, separate from the storefront customer account.
-- Username: forest_admin    Password: Forest2026!
-- BCrypt cost 12. Existing accounts, roles and passwords are never overwritten.
SET NAMES utf8mb4;
USE fangcun_forest;

INSERT INTO store_user (username, password_hash, display_name, role, status)
SELECT 'forest_admin', '$2b$12$7zr67HpHjYH9OX4eANvI2.YMq4lpfpGvuwaS2bHvcldZisfWVUwUe',
       '森林管理员', 'ADMIN', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM store_user WHERE username = 'forest_admin');
