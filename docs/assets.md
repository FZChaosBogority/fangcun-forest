# 方寸森林视觉素材

三张背景、一张图标、一张首页写实展示图和九张商品图均使用内置 imagegen 生成，已复制到项目静态资源目录。原始人物参考图只用于图标与首页盆栽造型的身份参考，不加入仓库。三张背景实际均为 1122 × 1402 像素；本次十张首页/商品图片实际均为 1448 × 1086 像素（4:3），保留原始生成 PNG。

## 森林欢迎背景

路径：`forest-storefront/src/main/resources/static/images/forest-welcome.png`。

作为欢迎页左侧背景；文字和深色遮罩由 HTML/CSS 独立绘制，图片本身无文字。

最终提示词：

Use case: photorealistic-natural. Asset type: full-bleed background photograph for the LEFT half of a desktop ecology shop's welcome/login page, brand Fangcun Forest. Create a high-end natural macro editorial photograph, portrait 4:5 composition. A tiny tranquil forest of lush cushion moss, small delicate fern fronds, textured dark gray rocks, dewdrops and a subtle edge of a clear glass terrarium, organic and believable. Main fern fronds arc from middle-right into upper center; vivid but restrained moss green, olive and deep forest shadows, warm soft side light, shallow depth of field with creamy natural bokeh. Lower-left should be darker and visually quiet enough for large white typography to be overlaid later in HTML. Overall calming, tactile, sophisticated, botanical photography, not cartoon, not illustration, not fantasy. Full-frame photographic scene ONLY. Absolutely no text, letters, logo, labels, UI, buttons, frame, human, pet or watermark. Output a tall high-resolution image.

## 水彩微森林背景

路径：`forest-storefront/src/main/resources/static/images/forest-welcome-watercolor.png`。

第二页使用全新构图的纸感水彩插画，没有以原图作为编辑或生成参考。

最终提示词：

Generate a brand-new illustration from scratch, without any reference image. Use case: illustration-story. Website background for a desktop ecology shop named Fangcun Forest. EXACT output canvas 1122 pixels wide by 1402 pixels high, portrait 4:5; both dimensions are important for a matched background carousel. A quiet miniature forest garden painted in Japanese watercolor and gouache on subtle textured ivory paper: soft moss mounds, fine fern silhouettes, a small winding pale stream and a few rounded stones. Distinct HAND-PAINTED illustration, visibly different from photography, limited olive, sage, pine green, warm cream and muted ochre pigments, restrained imperfect brush edges, gentle paper granulation, calm sophisticated botanical art. Organic foliage and painted light in the upper-right and middle; the lower-left is a quiet deep pine-green painted wash with plenty of negative space for large white website text. Full bleed illustration, no picture frame. No typography, logo, letters, watermark, user interface, human or animal. Do NOT imitate or edit a supplied image; invent a new composition.

## 立体生态景观背景

路径：`forest-storefront/src/main/resources/static/images/forest-welcome-sculpture.png`。

第三页使用全新构图的极简立体景观，没有以原图作为编辑或生成参考。

最终提示词：

Generate a brand-new illustration from scratch, without any reference image. Use case: stylized-concept. Website background for a desktop ecology shop named Fangcun Forest. EXACT output canvas 1122 pixels wide by 1402 pixels high, portrait 4:5, matched carousel image size. Minimal sculptural 3D miniature ecology artwork: a single rounded clear-glass terrarium / open bowl containing simple stylized bonsai-like little trees, cushion moss shapes, a smooth pebble path and a few fern forms, on a pale warm sage pedestal. Stylized smooth matte clay and softly translucent glass, large clean simplified shapes, soft studio light, tasteful playful but sophisticated design; clearly NOT a photograph, NOT watercolor. A new imaginative composition with the miniature scene primarily upper-right and middle-right; quiet dark sage/pine-green backdrop and foreground toward lower-left for white HTML text. Muted forest-green, celadon, cream and gentle warm stone tones. Full bleed scene. No typography, logo, letters, watermark, UI, people or animals. Do NOT use any previous image as a reference or edit; invent everything from scratch.

## 首页盆栽展示与独立商品图

首页右侧使用 `forest-storefront/src/main/resources/static/images/forest-hero-realistic.png`：真实植物摄影风格的小树盆栽，双马尾、刘海、眼睛、露齿笑容与小舌头均由绿色树叶和修剪凹面组成。使用完整 4:3 构图及柔和圆角，不裁掉花盆和双马尾。原有网站小图标保持不变。

每件商品单独生成写实展示图，暖白桌面、灰绿色背景和柔和自然光保持协调。资源同步放在用户端与管理端的 `src/main/resources/static/images/products/`，列表、详情、购物车与新订单共用商品封面路径。历史订单的图片快照保留原值。

| SKU | 商品 | 公共资源路径 |
| --- | --- | --- |
| FC-FOREST-001 | 晨雾苔藓瓶 | `/images/products/fc-forest-001.png` |
| FC-MOON-001 | 月面微景观 | `/images/products/fc-moon-001.png` |
| FC-MOSS-001 | 雨林苔藓 | `/images/products/fc-moss-001.png` |
| FC-FERN-001 | 迷你波士顿蕨 | `/images/products/fc-fern-001.png` |
| FC-STONE-001 | 火山石造景包 | `/images/products/fc-stone-001.png` |
| FC-SOIL-001 | 赤玉土基质包 | `/images/products/fc-soil-001.png` |
| FC-TWEEZER-001 | 长柄造景镊子 | `/images/products/fc-tweezer-001.png` |
| FC-LIGHT-001 | 桌面植物补光灯 | `/images/products/fc-light-001.png` |
| FC-FOREST-002 | 林间溪谷生态瓶 | `/images/products/fc-forest-002.png` |

十张图片的**最终完整提示词、SKU 与仓库保存路径**见 [catalog-image-prompts.json](catalog-image-prompts.json)。提示词请求 1536 × 1152，工具最终输出实际为 1448 × 1086，记录与 HTML 比例按实际文件核对，不进行二次缩放或裁图。

空库种子 `database/02-seed.sql` 已引用独立图片。已有数据库使用可重复执行的 `database/05-product-images.sql`：只替换这九个 SKU 的原始 `/images/terrarium.svg` 占位封面和对应占位相册；自定义封面不覆盖，库存、价格、上下架状态与订单不改动。`scripts/Initialize-Database.ps1` 会自动应用该补丁，无需重建数据库。本机已应用，更新前后图片与业务字段快照保存在 Git 忽略的 `.local/assets/`。

已通过隔离 Edge 静态预览核对十张图片全部加载，桌面与 390px 窄屏没有横向溢出；慢响应、页面渐隐渐显、游客登录回跳、前进后退与减少动态效果检查通过，浏览器运行时异常为零。静态预览不代替真实 Spring Boot 运行验证，构建与启动由用户执行。

## 盆栽少女图标

用户已明确选择直接采用生成结果，不再去除图中的渐变或重绘。透明 PNG 用于登录页标识、商城导航和浏览器图标，并同步提供给管理端。

路径：

- `forest-storefront/src/main/resources/static/images/forest-mascot.png`
- `forest-admin/src/main/resources/static/images/forest-mascot.png`

最终提示词：

Use case: logo-brand. Create ONE minimal flat solid-color website icon for Fangcun Forest, a desktop ecology shop. Input image 1 is a character identity reference ONLY: use the front-view girl's recognizable fluffy twin ponytails, bangs with a small ahoge, and cheeky toothy grin with tiny tongue. Transform those features into a TOPIARY TREE, not a human portrait: a small potted tree with a short visible trunk, and its entire living green leafy crown pruned into the shape of that anime girl's HEAD including facial expression and BOTH voluminous hanging twin ponytails. Crown, bangs, ponytails, leaf-shaped ties, face, eyes and grin must all read as sculpted GREEN FOLIAGE. No skin, no orange/red hair, no clothing, no human body, no human neck. The face is a lighter green leafy plane, features are very simple dark forest-green shapes; a cheeky asymmetric grin with a few simplified pale-green tooth shapes and tiny leaf-green tongue. Recognizable leafy twin-tail silhouette is crucial. A short straight dark-green trunk emerges under the leafy head into a simple small rounded trapezoid flowerpot with a rim in muted sage/forest green. Balanced frontal centered icon, generous clean transparent padding, suitable for a small app icon or favicon. Extremely simple graphic design with large clean geometric-organic shapes and only 3 or 4 completely uniform solid GREEN colors. Crisp clean edges, no tiny leaf veins, no decorative texture. No gradients, no shadows, no 3D, no realism, no text, no letters, no watermark, no frame or background tile. The whole image is a single isolated plant mascot on a genuinely TRANSPARENT background. Square format, the icon fills about 80 percent of the square.
