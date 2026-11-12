# 需求与代码对应

本项目将宠物商城改为“方寸森林”桌面生态补给商城。课程中的 Category / Product 分类层次对应本项目的两级 category；Item 对应单 SKU 的 product；Account 对应 store_user。两个独立工程共享七张业务表，不通过彼此的 HTTP 接口修改库存。

本清单记录源码实现位置，**不代表真实运行验收已通过**。真实构建、业务事务和并发结果按 [验证清单](verification.md) 记录。实验报告和个人信息不在此仓库中。

| 需求 | 实现 | 主要位置 |
| --- | --- | --- |
| Spring IoC / MVC / MyBatis-Plus | 构造器注入、Controller → Service → Mapper、分页和事务 | 两端 `controller/`、`service/`、`mapper/`、`config/` |
| Spring AOP | Service 耗时与后台操作日志，不输出密码或收货信息 | 两端 `aspect/` |
| 公开欢迎页与游客浏览 | 账号入口、游客入口、三种背景及渐隐渐显 | 用户端 `auth/` 模板、`auth.js`、`ink-reveal.js` |
| 首页两组商品 | 新品按 created_at / id 倒序最多 3 件，精选按 sort_order / id 升序最多 4 件；仅 ON_SALE | `CatalogServiceImpl`、`CatalogController`、`catalog/home.html` |
| 两级分类与商品类型 | 四个生态主题一级分类、对应二级类型；侧栏、下拉及首页二级入口 | `CatalogServiceImpl.findProducts`、`catalog/list.html` |
| 名称搜索与分页 | 名称包含匹配、LIKE 特殊字符转义、默认 6 件、1—50 件、筛选条件保留 | `CatalogController`、`PageRequest` |
| 四种排序与售罄 | 人工排序、价格升降序、按录入时间排序；零库存可展示 | `ProductSort`、`CatalogServiceImpl` |
| 商品详情与相册 | 按商品 ID 读取；下架/不存在返回 404；本地图片加载失败回退占位图 | `catalog/detail.html`、`commerce.js` |
| 注册与验证码 | 按本项目要求保留注册、独立的一次性登录/注册验证码；服务器校验 | `AuthController`、`RegistrationService`、`CaptchaService` |
| 登录与访问控制 | 只接受对应角色且启用的账号、BCrypt、CSRF、独立 Session Cookie | 两端 `security/`、Security 配置 |
| 退出与登录回跳 | 原位退出确认；游客登录后回到商品/购物车/订单，不自动重放写操作 | `LoginNavigation`、`commerce.js` |
| 购物车持久化 | 同账号同商品合并、数量 1—99、最多 20 种；版本与归属校验 | `CartService`、`CartItemMapper` |
| 购物车数量徽标 | 后端汇总本账号全部条目数量，含失效条目；游客为 0 | `CurrentAccountAdvice`、`CartService.quantity` |
| 整车结算 | 无勾选分支；任一失效商品阻止整车结算；有效商品金额由后端计算 | `CartView`、`cart/index.html`、`OrderService.prepare` |
| 确认后购物车变化 | 再次核对整车条目集合、数量、版本、价格，新增条目也使旧确认失效 | `OrderService.place` |
| 原子下单与快照 | 同一事务扣库存、写订单与明细、清车；保存成交名称/封面/价格/地址 | `OrderService.place`、`ProductMapper.reserve` |
| 订单查询 | 本人订单、完整订单号精确匹配、五状态、默认 10 件分页、条件保留 | 用户端 `OrderController`、`OrderService.list`、`orders/list.html` |
| 模拟支付成功与失败 | 两个显式动作；失败不写支付凭证、不改库存；成功保留首次凭证与时间 | `orders/payment.html`、`OrderService.pay/failPayment` |
| 取消与库存归还 | 只取消待付款；同一事务按快照增量恢复库存，重复取消不重复恢复 | 两端 `OrderService.cancel` |
| 确认收货 | 仅本人 SHIPPED → COMPLETED，重复确认不重写首次时间 | 用户端 `OrderService.complete` |
| 后台分类维护 | 两级增删查改；编码和父子关系创建后固定；关联删除拒绝 | `CategoryService`、`CategoryController` |
| 后台商品维护 | 新增 DRAFT、修改资料、上架/下架、相册；SKU 固定，旧版本不覆盖新资料 | `ProductService`、`ProductController` |
| 商品封面上传 | 新增/编辑时选择 JPG/PNG 自动上传预览；5MB 限制、像素重编码、随机文件名、双端共用持久目录 | `CoverUploadController`、`CoverImageStorage`、两端 `UploadedImagesConfig` |
| 增量补货 | 锁定数据库当前商品后增量增加库存，不依赖页面旧库存或旧版本 | `ProductService.restock`、`ProductMapper.restock` |
| 商品删除 | 按指导书保留受限删除；仅下架且无购物车/历史订单引用时允许 | `ProductService.delete` |
| 用户信息与密码重置 | 普通用户资料、启停、密码重置；管理员只读；不提供角色提升 | `UserService`、`UserController` |
| 全站订单管理 | 完整订单号精确查询、状态分页、收货信息维护、待付款取消、已取消删除 | 管理端 `OrderService`、`orders/` |
| 发货 | 仅 PAID → SHIPPED，首次发货时间保留；重复操作不再次写库存 | 管理端 `OrderService.ship` |
| 数据与运行材料 | 共享 SQL、幂等图片补丁、本机配置忽略、Maven Wrapper 和双端命令 | `database/`、`scripts/`、`README.md` |

## 主题适配与实现范围

宠物主题的“宠物/用品”二级类型在本项目中对应实际的森林生态瓶、主题微景观、苔藓、蕨类、石材、基质、手作工具、补光设备，不使用与生态商城无关的宠物分类名。分类值仍来自数据库，并用于相同的父子查询规则。

根据本项目明确要求，注册、验证码、管理员重置商城用户密码、受限商品删除、受限订单删除、页面动画及独立商品图片均保留。个人资料编辑、本人改密、真实支付、退款、物流轨迹、优惠券、自动超时关单不作为本次已实现功能。账号页只展示安全信息和已有业务入口，不提供假编辑按钮。

课程实验允许更换业务主题。本主题采用独立的七表 SQL，不能声称与未提供的课程配套 DDL 完全相同；验收时应以本仓库 SQL 初始化独立数据库，不覆盖原有课程数据库。
