-- 只在 01-schema.sql 已完成且业务表为空时执行一次。
-- 固定 ID 用于开发核对，重复执行会因唯一键报错；不要用于重置已有业务数据。
-- 图像为本项目原创矢量示意图，不表示实物照片。
SET NAMES utf8mb4;
USE fangcun_forest;
START TRANSACTION;

INSERT INTO category (id, code, parent_id, name, sort_order) VALUES
(1, 'LANDSCAPES', NULL, '微景观', 10),
(2, 'PLANTS', NULL, '植物', 20),
(3, 'MATERIALS', NULL, '造景材料', 30),
(4, 'TOOLS', NULL, '养护工具', 40);

INSERT INTO category (id, code, parent_id, name, sort_order) VALUES
(11, 'FOREST_BOTTLES', 1, '森林生态瓶', 11),
(12, 'MINI_LANDSCAPES', 1, '主题微景观', 12),
(13, 'MOSSES', 2, '苔藓', 21),
(14, 'FERNS', 2, '蕨类', 22),
(15, 'STONES', 3, '造景石材', 31),
(16, 'SUBSTRATES', 3, '种植基质', 32),
(17, 'HAND_TOOLS', 4, '手作工具', 41),
(18, 'LIGHTS', 4, '补光设备', 42);

INSERT INTO product
(id, sku, category_id, name, summary, description, cover_image, price, stock, status, sort_order)
VALUES
(1, 'FC-FOREST-001', 11, '晨雾苔藓瓶', '把清晨的绿意装进一只玻璃瓶。',
 '苔藓、造景石与玻璃容器组成的桌面微景观。适合摆在书桌一角；具体养护信息以后续商品说明为准。',
 '/images/terrarium.svg', 89.00, 12, 'ON_SALE', 10),
(2, 'FC-MOON-001', 12, '月面微景观', '用灰白石材与绿意搭起一个小小星球。',
 '以月面意象为主题的微景观成品，采用单一规格销售。页面插画为教学示意。',
 '/images/terrarium.svg', 129.00, 8, 'ON_SALE', 20),
(3, 'FC-MOSS-001', 13, '雨林苔藓', '给微景观铺上一层柔软的绿色。',
 '用于桌面微景观造景的苔藓材料，一份为一个销售单位。',
 '/images/terrarium.svg', 39.90, 20, 'ON_SALE', 30),
(4, 'FC-FERN-001', 14, '迷你波士顿蕨', '让一片小小蕨叶舒展在桌边。',
 '桌面造景植物示例，按单盆销售，无多规格选项。',
 '/images/terrarium.svg', 25.00, 15, 'ON_SALE', 40),
(5, 'FC-STONE-001', 15, '火山石造景包', '粗粝的肌理，为小森林增添地形。',
 '用于微景观造景的石材包，按固定包装销售。',
 '/images/terrarium.svg', 18.00, 30, 'ON_SALE', 50),
(6, 'FC-SOIL-001', 16, '赤玉土基质包', '从一份基质开始布置你的微小世界。',
 '种植基质示例商品。当前为零库存样例，保留展示。',
 '/images/terrarium.svg', 22.00, 0, 'ON_SALE', 60),
(7, 'FC-TWEEZER-001', 17, '长柄造景镊子', '让每一株绿意，都落在合适的位置。',
 '用于微景观布置的长柄镊子，单件销售。',
 '/images/terrarium.svg', 19.90, 25, 'ON_SALE', 70),
(8, 'FC-LIGHT-001', 18, '桌面植物补光灯', '为桌面绿意留一盏灯。',
 '桌面补光设备示例商品，按单一款式销售。',
 '/images/terrarium.svg', 79.00, 10, 'ON_SALE', 80),
(9, 'FC-FOREST-002', 11, '林间溪谷生态瓶', '层叠石景与苔藓的桌面组合。',
 '下架示例：管理端可读取，用户端列表与详情不可展示。',
 '/images/terrarium.svg', 159.00, 6, 'DRAFT', 90);

INSERT INTO product_image (product_id, image_path, alt_text, sort_order) VALUES
(1, '/images/terrarium.svg', '晨雾苔藓瓶示意插画', 10),
(2, '/images/terrarium.svg', '月面微景观示意插画', 10);

-- 当前阶段未实现身份模块：不伪造可登录账号、不填充密码占位值。
-- 购物车与订单保持空表，登录及交易阶段再添加经验证的账号和业务数据。
COMMIT;
