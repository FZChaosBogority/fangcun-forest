# 方寸森林基础架构

## 目标与当前边界

本阶段为桌面生态补给站建立可扩展的 Java Web 工程。提供用户端浏览链路和管理端只读观察链路，代码已经写入，构建、启动及数据库执行结果由用户本地验证。当前不作为实验一全部 P0 完成的证明。

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

Controller 与 Service 均采用构造器注入。对象创建、依赖装配与生命周期由 Spring IoC 容器管理，没有通过 `new` 构造 Service 或 Mapper。Service 不读取 HttpSession、Model 或 Servlet 请求。

`@MapperScan` 注册 Mapper 接口代理。`@Configuration` 和 `@Bean` 注册分页与乐观锁插件；`@Component` 注册时间字段填充器和切面。

## Spring AOP 与事务

`ServiceTimingAspect` 使用 `@Aspect` 和 `@Around` 包围本工程公开 Service 方法。采用单调时钟记录耗时，无论成功或异常均打印结果。切面不吞异常、不输出参数、不把失败转换为成功。

当前查询 Service 使用 `@Transactional(readOnly = true)`。事务管理与耗时日志都通过 Spring 代理生效，后续写业务需要独立的 `@Transactional(rollbackFor = Exception.class)` 边界，不要用同类内部调用绕过代理。

乐观锁插件和实体 `@Version` 已配置，但尚无写接口，不能据此声称并发业务已完成。跨工程库存一致性要依靠数据库事务、条件更新和统一锁顺序，不能只使用 Java 进程锁。

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

分类自关联；商品属于分类并有多张图片；用户有购物车条目与订单；订单有多条快照明细。两端均提供这七张表的实体和 Mapper，尚未实现的业务没有伪造 Service 或 Controller。

`BaseEntity` 提供自增 ID 和创建/修改时间。时间填充使用 Asia/Shanghai。金额使用 BigDecimal 与 DECIMAL(12,2)。product、cart_item、sales_order 含版本字段。分类 `parent_key` 是数据库生成列，不映射到 Java 写入字段。

当前 SQL 为新主题草案。数据库可校验外键、唯一键、数量和基本状态取值；“只允许两级、商品只能归属二级、每车最多 20 种、订单合法迁移”等规则还须在后续写 Service 中实现，不能只依赖建表约束。

## 页面与数据边界

用户端只读取 ON_SALE 商品，库存为零时仍可展示；商品不可用返回 404。分类查询支持一级及其所有二级商品、单个二级商品。名称搜索去两端空白、最多 120 字符，使用绑定参数及 LIKE 元字符转义。分页默认 6 条，管理端默认 10 条，最大 50 条；页码小于 1 归一为 1；非法格式返回 400。

管理端展示数据库实际计数，没有静态销售额、会员数或假订单。当前分类总览读取有限的全部分类，商品列表使用数据库分页；大规模分类管理不在本阶段实现范围内。

页面仅通过 Thymeleaf 文本转义展示描述，未使用 `th:utext`。没有未完成的购买、支付或管理写按钮。示例商品共用原创 SVG 教学插画，后续可以替换为有来源说明的实物资源。

## 会话与访问控制

两端 Cookie 分别配置为 FOREST_USER_SESSION、FOREST_ADMIN_SESSION，均 HttpOnly、SameSite=Lax。默认只绑定本机。配置不同 Cookie 名只是会话隔离准备，当前没有登录、鉴权或 CSRF 实现，也没有有副作用的业务请求。

后续必须补齐：角色与启用状态后端校验、登录态有效性、个人购物车/订单归属校验、CSRF、退出登录。管理端在此之前保持只读，不能把当前预览页当作已完成的管理员模块。

## 后续交易约束

以下是后续应实现的设计要求，当前阶段没有对应写接口：

- 购物车入库，不放在 Session 中；单商品数量 1—99，每车最多 20 种。
- 整车确认和成交价格由服务端确定；车或价格变化后重新确认。
- 下单在一个事务内写订单与明细、扣库存、清理已核对购物车条目。
- 支付只改变状态与成功凭证，不再次扣库存。
- 取消待支付订单与库存恢复同事务，重复取消只恢复一次。
- 支付与取消竞争同一待支付状态，只有一方可成功。
- 订单明细使用成交快照，商品改价或下架不改写历史。
- 商品编辑不覆盖库存，独立补货在数据库最新库存上增量更新。
- 订单只允许 PENDING_PAYMENT → PAID → SHIPPED；待支付可转 CANCELLED；SHIPPED → COMPLETED 为选做。
- 登录、购物车、交易和跨工程并发分别补充相应验收证据。

## 本阶段交付状态

已经提供工程、配置、实体与 Mapper、只读 Service、页面、数据库草案和文档。只做源码与文件结构静态核对；没有执行构建、测试、运行或数据库脚本。后续测试结果应按真实操作填写。
