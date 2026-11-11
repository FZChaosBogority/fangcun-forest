# 方寸森林

「方寸森林」是以桌面微景观、植物、造景材料和养护工具为主题的生态补给商城。两个独立的 Spring Boot 工程共用一个 MySQL 数据库；用户端提供浏览购买链路，管理端提供管理员登录、分类/商品维护、用户管理和订单处理代码。

**Maven 构建、测试和应用启动由你执行。** 数据库初始化可使用下文的本机交互脚本；文中的预期结果不代表业务验收已经通过。

## 工程与技术栈

| 项目 | 内容 |
| --- | --- |
| 用户端 | `forest-storefront`，默认 `http://localhost:8080` |
| 管理端 | `forest-admin`，默认 `http://localhost:8081` |
| Java | 编译目标 Java 17，可使用兼容的 JDK 17—25 |
| 构建工具 | 自带 Maven Wrapper，固定 Maven 3.9.11，无需全局安装 Maven |
| Web | Spring Boot 3.5.16、Spring MVC、Thymeleaf、Spring Security |
| 持久化 | MyBatis-Plus 3.5.18、MySQL Connector/J、HikariCP |
| 数据库 | MySQL 8.0.16+，库名 `fangcun_forest` |
| 公共能力 | 构造器注入、Service 读写事务、AOP 耗时/管理操作日志、分页、版本检查、时间字段填充、异常页 |

两个工程分别使用 Spring Boot 官方父 POM，没有相互依赖或公共 JAR，可以独立导入、构建和运行。根目录不是 Maven 聚合工程，命令需要指定对应的 `pom.xml`。

## 当前已提供的代码

- 用户端商品：左右分栏欢迎页、首页搜索、两级分类筛选、名称搜索、价格/时间排序、有货筛选、数据库分页、商品详情及相册切换；下架商品不展示，零库存显示售罄。
- 用户端账号：注册、登录、退出、个人入口，BCrypt 密码散列与两分钟有效的一次性图片验证码。
- 用户端交易：持久化购物车、数量修改和移除、勾选结算、收货信息、事务下单、订单列表和详情、取消归还库存、模拟支付、已发货订单确认收货。
- 管理端：ADMIN 身份隔离、实时统计、两级分类增删改查、商品增删改查/上下架/相册/增量补货、商城用户资料和状态维护/密码重置、订单查询/收货信息编辑/取消/删除已取消订单/发货。
- 两端：七张表的实体与 Mapper、MVC 分层、统一错误页、数据源及独立 Session Cookie 配置。
- 数据库：七张表结构草案、12 个分类、9 件商品、2 条图片记录及独立的教学商城账号与管理员账号，包含零库存和下架样例。
- 页面资源：本地 CSS、森林图片、用户选定的盆栽少女 PNG 图标与商品 SVG 示意图，不依赖 CDN。

用户端交易金额由服务器计算，购物车与订单按当前账号隔离；管理端写操作要求 ADMIN 和 CSRF。模拟支付不接真实资金。代码已提供，构建、业务运行、事务回滚与跨工程并发效果仍须用户验证。

本次继续使用原有七张表，已有完整数据库不需要重建；首次启用后台请执行 `database/04-demo-admin.sql` 添加管理员，不覆盖既有账号。详细开启命令与后台验收见 [管理端说明](docs/admin.md)，购买流程见 [用户端说明](docs/storefront.md)。

用户端根路径现为欢迎页，账号入口为 `/login`，游客入口和商城首页为 `/main`。教学账号：`forest`，密码：`Forest2026!`，和 MySQL 的 root 账号无关。功能与手工检查见 [登录页说明](docs/login.md)。

后台入口为 `http://localhost:8081/`，教学管理员 `forest_admin / Forest2026!`。两端是不同角色和会话。商品页退出通过附近的确认气泡原位切为游客；游客加购先渐隐进入登录，成功后返回原商品页。

## 目录

```text
MyStore/
├── mvnw / mvnw.cmd         # 项目自带 Maven 入口
├── .mvn/wrapper/           # 固定 Maven 版本、下载校验与许可证
├── forest-storefront/       # 独立用户端 Maven 工程
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/fangcun/forest/storefront/
│       │   ├── aspect/      # Service 耗时切面
│       │   ├── common/      # 异常、分页参数、异常处理
│       │   ├── config/      # MyBatis-Plus、自动填充
│       │   ├── controller/  # 请求与视图
│       │   ├── entity/      # 七张表映射
│       │   ├── enums/       # 商品、账号、订单状态
│       │   ├── mapper/      # BaseMapper 数据访问
│       │   └── service/     # 业务接口与实现
│       └── resources/      # 配置、templates、static
├── forest-admin/            # 独立管理端工程，分层同上
├── database/
│   ├── 01-schema.sql        # 先建表
│   ├── 02-seed.sql          # 再导入本主题示例
│   ├── 03-demo-account.sql  # 添加教学商城账号，不覆盖已有账号
│   ├── 04-demo-admin.sql    # 添加教学管理员，不覆盖已有账号
│   └── 05-product-images.sql # 将原始商品占位图换为独立展示图
└── docs/
    ├── architecture.md
    └── manual-checklist.md
```

## 1 准备数据库

Windows 可在项目根目录执行下面的命令。脚本会在本机提示输入 MySQL 密码，不把密码写入命令历史；创建空库和七张表，仅在全部业务表为空时导入示例数据，最后查询行数验证。已有数据不会被删除，已有本机配置不会被覆盖。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize-Database.ps1 -ConfigureApplications
```

脚本自动查找 MySQL 客户端，找不到时可增加 `-MySqlPath 'C:\实际安装目录\bin\mysql.exe'`；默认使用 `localhost:3306` 的 `root` 账号，可通过 `-DatabaseHost`、`-Port`、`-User` 调整。`-ConfigureApplications` 会把刚输入的连接信息写入两个 Git 已忽略的 `application-local.yml`；不希望保存连接信息时省略此参数。`.local/database-init/` 下的结果文件只记录初始化状态与行数，不记录密码。

也可以按以下步骤手动初始化。

当前目录没有课程 SQL，因此这里的 SQL 是面向新主题的独立设计草案，**不声称与课程 DDL 完全一致**。后续拿到课程 SQL 后，统一核对表字段、约束、种子数据与验收口径，再同步修改两端实体。不要直接对已有课程数据库运行本草案。

在新的空库环境中，用 Navicat、MySQL Workbench 或命令行依次执行：

1. `database/01-schema.sql`
2. `database/02-seed.sql`
3. `database/03-demo-account.sql`
4. `database/04-demo-admin.sql`
5. `database/05-product-images.sql`

如果使用命令行，从项目根目录进入 MySQL：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

然后在 MySQL 客户端内执行：

```sql
SOURCE database/01-schema.sql;
SOURCE database/02-seed.sql;
SOURCE database/03-demo-account.sql;
SOURCE database/04-demo-admin.sql;
SOURCE database/05-product-images.sql;
```

这里的文件是“结构 + 商品数据 + 教学用户 + 教学管理员 + 图片补丁”的先后关系。与课程结构版/完整初始化版二选一的脚本约定不同。脚本不清库，应用配置 `spring.sql.init.mode=never`，不会在启动时自动建表或重置库存。02 数据脚本仅执行一次。已有完整库补齐需要的 03/04 即可，已有同名账号的密码、角色与状态保持不变。05 只替换九件示例商品的原始占位图片，不覆盖自定义封面或调整业务数据，可重复运行；素材与提示词见 [图片说明](docs/assets.md)。本机数据库已应用图片补丁，无需重新初始化。

## 2 配置本机数据库连接

在两个工程各自的 `src/main/resources` 中，将 `application-local.example.yml` 复制为 `application-local.yml`，填写你的 MySQL 用户名和密码。这两个实际配置文件均已被 Git 忽略：

```powershell
Copy-Item forest-storefront/src/main/resources/application-local.example.yml forest-storefront/src/main/resources/application-local.yml
Copy-Item forest-admin/src/main/resources/application-local.example.yml forest-admin/src/main/resources/application-local.yml
```

也可以不复制文件，改为在各自启动终端或 IDEA Run Configuration 中设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。使用本地配置文件时，其数据源设置覆盖主配置中的环境变量占位值，二选一即可。

默认只绑定 `127.0.0.1`；默认端口为 8080 和 8081，均可用 `SERVER_PORT` 覆盖。两端连接必须指向同一库。

## 3 在 IDEA 中导入

1. 打开根目录，再将 `forest-storefront/pom.xml` 和 `forest-admin/pom.xml` 分别添加为 Maven 项目。
2. 选择 JDK 17—25，等待各自的 Maven 依赖导入。
3. 完成数据库初始化和本机配置。
4. 分别运行 `ForestStorefrontApplication` 和 `ForestAdminApplication`。

本机检测到 JDK 25，未检测到 PATH 中的 `mvn` 命令。项目已提供 Maven Wrapper，不使用 IDEA 也能启动，不需要把 IDEA 的 JBR 路径写死在代码或 POM 中。

## 4 命令行构建与启动

以下步骤由你执行。在 PowerShell 中先进入项目根目录，再使用当前目录下的 `.\mvnw.cmd`：

```powershell
cd D:\上机实验\MyStore
```

Windows 使用 `.\mvnw.cmd` 替代 `mvn`；macOS / Linux 使用 `sh ./mvnw`。首次使用会从 Apache Maven Central 下载固定版本 Maven，缓存到用户目录下的 `.m2/wrapper/dists`，需要联网；脚本会校验已固定的 SHA-256。后续继续复用缓存，Java 和 MySQL 仍需事先配置。

两个工程需要分别构建：

```powershell
.\mvnw.cmd -f forest-storefront/pom.xml clean package
.\mvnw.cmd -f forest-admin/pom.xml clean package
```

用户端已提供登录相关的 MockMvc，以及注册、验证码、购物车和订单规则测试，本次未执行。构建成功不能替代真实数据库和页面业务验收。

启动时，分别在两个终端中运行：

```powershell
# 终端一，当前目录为项目根目录
.\mvnw.cmd -f forest-storefront/pom.xml spring-boot:run
```

```powershell
# 终端二，当前目录为项目根目录
.\mvnw.cmd -f forest-admin/pom.xml spring-boot:run
```

或在构建后分别使用 `java -jar forest-storefront/target/forest-storefront-0.0.1-SNAPSHOT.jar` 和 `java -jar forest-admin/target/forest-admin-0.0.1-SNAPSHOT.jar`。

如果 PowerShell 提示“无法识别 mvn”，改用上面的 `.\mvnw.cmd`，无需修改系统 PATH。如果 Wrapper 的首次下载失败，检查网络连接与终端错误信息；不要反复执行旧的 `mvn` 命令。Maven 开始工作后，仍需等待项目依赖下载；数据库未初始化或连接未配置时，页面不能正常读取商品数据。

## 5 访问路径与基础验证

| 地址 | 内容 |
| --- | --- |
| `http://localhost:8080/` 或 `/index` | 欢迎入口页，账号登录与游客入口 |
| `http://localhost:8080/login` | 商城账号表单，支持登录失败与退出提示 |
| `http://localhost:8080/register` | 创建商城账号，带图片验证码 |
| `http://localhost:8080/account` | 当前账号入口 |
| `http://localhost:8080/main` | 商城首页，读取分类和最多 4 件精选商品 |
| `http://localhost:8080/category` | 商品列表，支持 `categoryId`、`keyword`、`page`、`size` |
| `http://localhost:8080/product?id=1` | 商品详情 |
| `http://localhost:8080/cart` | 账号购物车与勾选结算 |
| `http://localhost:8080/orders` | 当前账号订单列表 |
| `http://localhost:8080/orders/{id}` | 订单详情、支付、取消与确认收货入口 |
| `http://localhost:8081/` | 管理概览，要求管理员登录 |
| `http://localhost:8081/products` | 全部商品分页，含下架商品 |
| `http://localhost:8081/categories` | 两级分类维护 |
| `http://localhost:8081/users` | 用户资料与密码管理 |
| `http://localhost:8081/orders` | 订单处理与发货 |
| 两端的 `/actuator/health` | 健康检查，含数据库连接检查；不公开详细连接配置 |

首次按 01—04 导入后的预期：分类 12 个（一级 4 个、二级 8 个），商品 9 件，其中上架 8 件，普通教学账号和管理员各 1 个，订单 0 张；用户端默认每页 6 件，商品 6 为零库存样例，商品 9 为下架样例。用户端不显示商品 9，直接请求详情应返回 404。重启后数据应保留。

完整的手工检查步骤见 [基础检查清单](docs/manual-checklist.md)。AOP 日志形如 `service=CatalogServiceImpl.findProducts(..) success=true elapsedMs=...`，不记录用户参数或响应数据。

## 后续开发顺序

1. 由用户构建、启动并验证完整购买流程，记录真实事务回滚与并发结果。
2. 验证管理员登录、分类/商品写操作、增量补货、发货和双端身份隔离。
3. 核对课程 SQL 与更换主题后的 P0 范围，完成实验报告。

真实支付、邮件/短信验证码、找回密码、优惠券和物流轨迹不在本次实现范围内。待付款订单目前由用户主动取消，没有自动超时释放库存。详见 [架构与实现边界](docs/architecture.md)。

## 版本依据

- [Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [MyBatis-Plus 安装与 Spring Boot 3 Starter](https://baomidou.com/getting-started/install/)
- [MyBatis-Plus 分页插件](https://baomidou.com/plugins/pagination/)：分页解析器从 3.5.9 起需要独立依赖，本项目已声明。
- [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/)：使用官方 3.3.4 only-script 脚本，保留上游许可证头；固定的 Maven 3.9.11 归档对应许可证和 NOTICE 位于 `.mvn/wrapper/`。
- [Spring Security 表单登录](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/form.html)：真实表单验证与 Thymeleaf CSRF 集成。
