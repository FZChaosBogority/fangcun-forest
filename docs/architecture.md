# 方寸森林基础架构

## 目标与当前边界

本阶段提供桌面生态补给站的商品、账号与购买链路，以及分类、商品、用户和订单后台管理代码。构建、启动与业务测试由用户本地验证；数据库可通过本机初始化脚本显式创建与核对。代码交付不作为实验一全部 P0 验收通过的证明。

用户端包为 `com.fangcun.forest.storefront`，管理端为 `com.fangcun.forest.admin`。两工程通过同一 MySQL 数据源共享业务数据，不通过 HTTP 调用购买、补货业务，也不引入微服务、公共 JAR 或额外部署平台。

## 请求链路与 Spring IoC

```text
浏览器 GET 请求
  → Spring MVC Controller（解析参数、组织 Model、返回模板名）
  → Service 接口
  → @Service 实现（规则与事务边界）
  → MyBatis-Plus Mapper（BaseMapper）
  → MySQL
  → Thymeleaf 模板
  → HTML 响应
```

Controller 与 Service 均采用构造器注入。对象创建、依赖装配与生命周期由 Spring IoC 容器管理，没有通过 `new` 构造 Service 或 Mapper。交易 Service 不读取 HttpSession、Model 或 Servlet 请求；验证码 Service 使用 Session 保存短期挑战，结算凭证由 web 层的 CheckoutTokens 管理。

`@MapperScan` 注册 Mapper 接口代理。`@Configuration` 和 `@Bean` 注册分页与乐观锁插件；`@Component` 注册时间字段填充器和切面。

## Spring AOP 与事务

`ServiceTimingAspect` 使用 `@Aspect` 和 `@Around` 包围本工程公开 Service 方法。采用单调时钟记录耗时，无论成功或异常均打印结果。切面不吞异常、不输出参数、不把失败转换为成功。

查询 Service 使用 `@Transactional(readOnly = true)`，购物车、注册及订单写方法使用独立的 `@Transactional`，业务失败抛出运行时 BusinessException，由事务代理回滚。日志切面不输出参数或结果，避免记录密码、验证码和收货信息。

乐观锁插件和实体 `@Version` 保留。购物车与订单写操作显式携带归属和版本条件；自定义库存 SQL 手工增加版本号。写交易先锁定当前用户行，再按商品 ID 升序处理库存，结合数据库事务与条件更新，覆盖空购物车插入、重复下单及支付/取消竞争。真实并发效果仍需 MySQL 验证。

## 数据模型

| 表 | 用途 |
| --- | --- |
| store_user | 用户名、密码散列、显示名、角色、启停状态 |
| category | 固定两级分类；一级为空父分类，商品归属二级 |
| product | 单 SKU 商品、价格、库存、上下架与版本 |
| product_image | 商品相册路径、替代文本和排序 |
| cart_item | 账号购物车条目、数量、版本 |
| sales_order | 订单状态、金额、收货快照、模拟支付信息与版本 |
| sales_order_item | 商品标识、名称、封面与成交价格快照 |

分类自关联；商品属于分类并有多张图片；用户有购物车条目与订单；订单有多条快照明细。本次复用七张表，不要求重建数据库。

`BaseEntity` 提供自增 ID 和创建/修改时间。时间填充使用 Asia/Shanghai。金额使用 BigDecimal 与 DECIMAL(12,2)。product、cart_item、sales_order 含版本字段。分类 `parent_key` 是数据库生成列，不映射到 Java 写入字段。

当前 SQL 为新主题草案。数据库校验外键、唯一键、数量和基本状态取值；每车最多 20 种及订单合法迁移由 Service 校验。管理端校验两级分类、商品归属、引用删除、编辑版本等规则。分类结构写操作锁定分类树，商品新建/编辑先锁分类，再锁商品；库存只在独立增量 SQL 中变更。

## 页面与数据边界

用户端只读取 ON_SALE 商品，库存为零时仍可展示；商品不可用返回 404。分类查询支持一级及其所有二级商品、单个二级商品。名称搜索去两端空白、最多 120 字符，使用绑定参数及 LIKE 元字符转义。分页默认 6 条，管理端默认 10 条，最大 50 条；页码小于 1 归一为 1；非法格式返回 400。

管理端展示数据库实际计数，没有静态销售额、会员数或假订单。当前分类总览读取有限的全部分类，商品列表使用数据库分页；大规模分类管理不在本阶段实现范围内。

页面通过 Thymeleaf 文本转义展示描述，未使用 `th:utext`。两端业务按钮均连接服务器，管理写操作带 CSRF。示例商品共用原创 SVG 教学插画，管理端可维护本地图片资源路径。

## 会话与访问控制

两端 Cookie 分别配置为 FOREST_USER_SESSION、FOREST_ADMIN_SESSION，均 HttpOnly、SameSite=Lax。默认只绑定本机。两端均接入 Spring Security 数据库账号验证、BCrypt、CSRF、Session ID 更新与 POST 退出。后台仅接受 ADMIN；用户端仅接受 CUSTOMER。

用户端提供注册、独立登录/注册图片验证码、个人购物车与订单归属校验；注册始终创建 CUSTOMER。后台维护普通用户资料、状态和密码，不在普通用户表单中修改角色或管理员密码。会话保存密码散列指纹，动态请求核对数据库当前指纹；禁用、改角色或重置密码后旧会话失效。

## 当前交易约束

以下规则已在用户端代码实现，仍待用户运行验证：

- 购物车入库，不放在 Session 中；单商品数量 1—99，每车最多 20 种。
- 勾选结算和成交价格由服务端确定；车或价格变化后重新确认。Session 保留最多 6 份、30 分钟有效的结算快照，同一快照重复提交返回同一订单。
- 下单在一个事务内写订单与明细、扣库存、清理已核对购物车条目。
- 支付只改变状态与成功凭证，不再次扣库存。
- 取消待支付订单与库存恢复同事务，重复取消只恢复一次。
- 支付与取消竞争同一待支付状态，只有一方可成功。
- 订单明细使用成交快照，商品改价或下架不改写历史。
- 用户端提供 PENDING_PAYMENT → PAID、PENDING_PAYMENT → CANCELLED、SHIPPED → COMPLETED；管理端提供 PAID → SHIPPED、PENDING_PAYMENT → CANCELLED。
- 模拟支付只使用 MOCK，界面明确无真实扣款；待支付订单未实现自动超时取消。

管理端商品编辑不写库存，补货在最新库存上增量更新；发货不再次扣库存。后台订单先锁所属用户，再锁订单，取消时按商品 ID 升序归还库存。仅删除已取消订单，保留已付款和履约记录。跨工程竞争仍须用户在真实 MySQL 下验证。

## 本阶段交付状态

已经提供工程、配置、实体与 Mapper、浏览与交易 Service、页面、数据库草案和文档，以及供用户运行的业务测试。Maven 构建、测试与应用启动仍由用户执行。数据库初始化通过 scripts/Initialize-Database.ps1 显式操作，实际状态和行数保存在已被 Git 忽略的 .local/database-init/ 下，不将初始化结果当作业务验收通过。详细路径和验收步骤见 [用户端功能说明](storefront.md)。
