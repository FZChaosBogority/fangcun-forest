# 方寸森林

「方寸森林」是以桌面微景观、植物、造景材料和养护工具为主题的生态补给商城。当前交付基础框架：两个独立的 Spring Boot 工程，共用一个 MySQL 数据库，提供读取真实数据的页面，供后续登录、购物车与订单开发使用。

**按你的要求，未执行 Maven 构建、测试、应用启动或数据库初始化。** 文中的检查步骤和预期结果供你本地验证，不代表已通过运行验收。

## 工程与技术栈

| 项目 | 内容 |
| --- | --- |
| 用户端 | `forest-storefront`，默认 `http://localhost:8080` |
| 管理端 | `forest-admin`，默认 `http://localhost:8081` |
| Java | 编译目标 Java 17，可使用兼容的 JDK 17—25 |
| 构建工具 | Maven 3.6.3+，建议使用 IDEA 内置 Maven |
| Web | Spring Boot 3.5.16、Spring MVC、Thymeleaf |
| 持久化 | MyBatis-Plus 3.5.18、MySQL Connector/J、HikariCP |
| 数据库 | MySQL 8.0.16+，库名 `fangcun_forest` |
| 公共能力 | 构造器注入、Service 只读事务、AOP 耗时日志、分页、乐观锁、时间字段填充、异常页 |

两个工程分别使用 Spring Boot 官方父 POM，没有相互依赖或公共 JAR，可以独立导入、构建和运行。根目录不是 Maven 聚合工程，命令需要指定对应的 `pom.xml`。

## 当前已提供的代码

- 用户端：首页、一级分类筛选与名称搜索、数据库分页、商品详情及相册读取；下架商品不展示，零库存显示售罄。
- 管理端：真实数据库计数、全状态商品分页列表、两级分类只读列表。
- 两端：七张表的实体与 Mapper、MVC 分层、统一错误页、数据源及独立 Session Cookie 配置。
- 数据库：七张表结构草案、12 个分类、9 件商品、2 条图片记录，包含零库存和下架样例。
- 页面资源：本地 CSS 与原创 SVG 示意图，不依赖 CDN。

**当前管理端为本地只读预览，尚未实现管理员登录与鉴权；无新增、修改、删除、补货或发货接口。** 用户登录、购物车、订单、模拟支付同样尚未实现。已配置不同的 Cookie 名，但实际身份隔离仍需登录模块和运行验证。

## 目录

```text
MyStore/
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
│   └── 02-seed.sql          # 再导入本主题示例
└── docs/
    ├── architecture.md
    └── manual-checklist.md
```

## 1 准备数据库

当前目录没有课程 SQL，因此这里的 SQL 是面向新主题的独立设计草案，**不声称与课程 DDL 完全一致**。后续拿到课程 SQL 后，统一核对表字段、约束、种子数据与验收口径，再同步修改两端实体。不要直接对已有课程数据库运行本草案。

在新的空库环境中，用 Navicat、MySQL Workbench 或命令行依次执行：

1. `database/01-schema.sql`
2. `database/02-seed.sql`

如果使用命令行，从项目根目录进入 MySQL：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

然后在 MySQL 客户端内执行：

```sql
SOURCE database/01-schema.sql;
SOURCE database/02-seed.sql;
```

这里的两个文件是“结构 + 数据”的先后关系。与课程结构版/完整初始化版二选一的脚本约定不同。脚本不清库，应用配置 `spring.sql.init.mode=never`，不会在启动时自动建表或重置库存。数据脚本仅执行一次，不用于恢复已有交易数据。

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

本机检测到 JDK 25，未检测到 PATH 中的 `mvn` 命令；可以使用 IDEA 内置 Maven。项目没有 Maven Wrapper，不需要把 IDEA 的 JBR 路径写死在代码或 POM 中。

## 4 命令行构建与启动

以下步骤由你执行。两个工程需要分别构建：

```powershell
mvn -f forest-storefront/pom.xml clean package
mvn -f forest-admin/pom.xml clean package
```

基础框架尚未提供自动化测试用例；构建成功仅表示编译和打包成功，不能作为业务验收通过的证据。

启动时，分别在两个终端中运行：

```powershell
# 终端一，当前目录为项目根目录
mvn -f forest-storefront/pom.xml spring-boot:run
```

```powershell
# 终端二，当前目录为项目根目录
mvn -f forest-admin/pom.xml spring-boot:run
```

或在构建后分别使用 `java -jar forest-storefront/target/forest-storefront-0.0.1-SNAPSHOT.jar` 和 `java -jar forest-admin/target/forest-admin-0.0.1-SNAPSHOT.jar`。

## 5 访问路径与基础验证

| 地址 | 内容 |
| --- | --- |
| `http://localhost:8080/` 或 `/main` | 用户端首页，读取分类和最多 4 件精选商品 |
| `http://localhost:8080/category` | 商品列表，支持 `categoryId`、`keyword`、`page`、`size` |
| `http://localhost:8080/product?id=1` | 商品详情 |
| `http://localhost:8081/` | 管理端只读概览 |
| `http://localhost:8081/products` | 全部商品分页，含下架商品 |
| `http://localhost:8081/categories` | 分类只读列表 |
| 两端的 `/actuator/health` | 健康检查，含数据库连接检查；不公开详细连接配置 |

首次数据导入后的预期：分类 12 个（一级 4 个、二级 8 个），商品 9 件，其中上架 8 件，订单 0 张；用户端默认每页 6 件，商品 6 为零库存样例，商品 9 为下架样例。用户端不显示商品 9，直接请求详情应返回 404。重启后数据应保留。

完整的手工检查步骤见 [基础检查清单](docs/manual-checklist.md)。AOP 日志形如 `service=CatalogServiceImpl.findProducts(..) success=true elapsedMs=...`，不记录用户参数或响应数据。

## 后续开发顺序

1. 核对课程 SQL 与更换主题后的 P0 范围，完善商品页面与边界。
2. 用户端和管理员登录、密码散列、角色/禁用状态检查、独立 Session、CSRF。
3. 购物车持久化、数量和种类上限、数据归属及并发版本检查。
4. 订单确认、快照、事务下单、取消与库存恢复。
5. 模拟支付、后台分类/商品写操作、增量补货、发货。
6. 补充业务测试、真实事务回滚与跨工程并发证据，完成实验报告。

注册、真实支付、优惠券、物流轨迹等不在当前基础框架内。详见 [架构与实现边界](docs/architecture.md)。

## 版本依据

- [Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [MyBatis-Plus 安装与 Spring Boot 3 Starter](https://baomidou.com/getting-started/install/)
- [MyBatis-Plus 分页插件](https://baomidou.com/plugins/pagination/)：分页解析器从 3.5.9 起需要独立依赖，本项目已声明。
