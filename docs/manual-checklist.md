# 基础框架手工检查清单

以下步骤全部待执行。用户负责构建与启动，不能将此清单当作已通过的测试记录。新环境按 01、02、03、04、05 初始化 SQL，再填写两端数据库配置；已有数据库无需重建，只补齐管理员账号脚本。

## 构建与启动

- [ ] 未全局安装 Maven 时，先在根目录执行 `.\mvnw.cmd -v`，确认固定版本可下载并显示；此步骤由用户执行。
- [ ] 两个 POM 分别导入成功，依赖版本可解析。
- [ ] 分别执行 README 中两条 package 命令，记录实际结果。
- [ ] 用户端绑定 8080，管理端绑定 8081，可同时运行。
- [ ] 两端 /actuator/health 返回 UP；连接失败时不能记录为通过。
- [ ] 日志无 Mapper 扫描、驱动或模板解析错误。
- [ ] 一次页面查询后出现 Service 耗时日志，成功字段与实际结果一致。

## 初始化数据

- [ ] category 共 12 行，4 个一级、8 个二级。
- [ ] product 共 9 行，ON_SALE 8 行、DRAFT 1 行。
- [ ] product_image 共 2 行，03/04 脚本分别添加 forest 用户和 forest_admin 管理员，购物车、订单及订单明细为空。
- [ ] 重启应用不会清库、重建表或再次插入商品。

可用下面的只读 SQL 核对：

```sql
USE fangcun_forest;
SELECT COUNT(*) FROM category;
SELECT status, COUNT(*) FROM product GROUP BY status;
SELECT id, sku, stock, version FROM product ORDER BY id;
SELECT COUNT(*) FROM sales_order;
```

## 用户端浏览

- [ ] 首页显示 4 个一级分类及二级入口；新品按 created_at、id 倒序最多 3 件；精选按 sort_order、id 升序最多 4 件。
- [ ] 默认 /category 第一页 6 件，第二页 2 件。
- [ ] /category?categoryId=1 只显示该一级下的上架微景观商品。
- [ ] /category?categoryId=11 只显示“森林生态瓶”的上架商品。
- [ ] /category?keyword=苔藓 搜索后翻页仍保留关键词。
- [ ] /category?keyword=%25 按字面百分号查询，不作为通配符返回全表。
- [ ] /product?id=1 返回真实商品、分类、金额和图片。
- [ ] /product?id=6 显示零库存和售罄状态。
- [ ] /product?id=3 无相册仍显示封面，没有空相册区。
- [ ] /product?id=9 下架商品返回 404，不泄露商品详情。
- [ ] /product?id=999999 返回 404。

## 参数、空结果与技术异常

- [ ] /category?page=0 按第一页展示。
- [ ] /category?size=0 与 size=51 返回 HTTP 400。
- [ ] /category?page=abc 与 /product?id=abc 返回 HTTP 400。
- [ ] /category?categoryId=999999 返回 HTTP 404。
- [ ] 超长关键词返回 HTTP 400。
- [ ] /category?page=999999 显示空结果并提供返回第一页。
- [ ] 名称无匹配结果时提供清空筛选入口。
- [ ] 不存在路径返回 HTTP 404，错误页不输出堆栈。
- [ ] 在可恢复的本地环境模拟数据库不可用，页面返回失败提示而不是“暂无商品”，健康检查不是 UP。

## 管理端

- [ ] / 的真实计数为 12 个分类、9 件商品、8 件上架、0 张订单。
- [ ] /products 包含下架商品 9，价格、库存与数据库一致。
- [ ] /products?size=2 可翻页，最多接受 50 条。
- [ ] /categories 能看到两级分类及父分类编号。
- [ ] 管理员可登录，普通商城用户不能登录后台；未登录不能打开管理页面。
- [ ] 分类和商品可维护、上下架、补货，引用删除和过期版本有明确提示。
- [ ] 用户资料、状态和密码可维护，密码重置后旧会话失效。
- [ ] 已付款订单可发货，用户可确认收货；已取消订单可删除，库存不会重复归还。
- [ ] 完整边界与跨工程验收按 [管理端说明](admin.md) 记录真实结果。

## 界面与后续验收

- [ ] Chrome 或 Edge 的 1366×768、1920×1080 主页面可用。
- [ ] 无外部 CDN 依赖，CSS、PNG 和占位 SVG 路径正常。
- [ ] 记录实际 JDK、Maven、MySQL、浏览器版本和验证结果。

用户端商品、注册登录、验证码、购物车、订单和模拟支付代码已提供，完整操作与边界检查见 [用户端功能说明](storefront.md)。尚未执行 Maven 测试或完整运行验收；管理员登录、双端身份隔离、事务回滚、支付/取消竞争和跨工程并发均须真实运行后才能记为通过。
