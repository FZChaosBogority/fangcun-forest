-- Replace only the original teaching catalog placeholders. Safe to run again.
-- Existing custom covers, prices, stock, status, accounts and orders are preserved.
SET NAMES utf8mb4;
USE fangcun_forest;
START TRANSACTION;

UPDATE product
SET cover_image = CASE sku
    WHEN 'FC-FOREST-001' THEN '/images/products/fc-forest-001.png'
    WHEN 'FC-MOON-001' THEN '/images/products/fc-moon-001.png'
    WHEN 'FC-MOSS-001' THEN '/images/products/fc-moss-001.png'
    WHEN 'FC-FERN-001' THEN '/images/products/fc-fern-001.png'
    WHEN 'FC-STONE-001' THEN '/images/products/fc-stone-001.png'
    WHEN 'FC-SOIL-001' THEN '/images/products/fc-soil-001.png'
    WHEN 'FC-TWEEZER-001' THEN '/images/products/fc-tweezer-001.png'
    WHEN 'FC-LIGHT-001' THEN '/images/products/fc-light-001.png'
    WHEN 'FC-FOREST-002' THEN '/images/products/fc-forest-002.png'
    ELSE cover_image
END,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE sku IN ('FC-FOREST-001', 'FC-MOON-001', 'FC-MOSS-001', 'FC-FERN-001', 'FC-STONE-001', 'FC-SOIL-001', 'FC-TWEEZER-001', 'FC-LIGHT-001', 'FC-FOREST-002')
  AND cover_image = '/images/terrarium.svg';

UPDATE product_image AS pi
JOIN product AS p ON p.id = pi.product_id
SET pi.image_path = p.cover_image,
    pi.alt_text = CONCAT(p.name, '展示图')
WHERE p.sku IN ('FC-FOREST-001', 'FC-MOON-001', 'FC-MOSS-001', 'FC-FERN-001', 'FC-STONE-001', 'FC-SOIL-001', 'FC-TWEEZER-001', 'FC-LIGHT-001', 'FC-FOREST-002')
  AND pi.image_path = '/images/terrarium.svg'
  AND p.cover_image = CONCAT('/images/products/', LOWER(p.sku), '.png');

COMMIT;
