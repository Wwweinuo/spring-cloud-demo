# spring-cloud-demo

CloudMall 当前按照推荐的微服务多模块结构组织。三个业务服务拥有独立的启动类、端口、配置、Controller、Service、Mapper 和数据边界；当前仍暂不接入 Nacos、OpenFeign、Gateway、Sentinel、Seata、MQ 等治理组件。

## 技术约束

- JDK 25
- Maven 3.9+
- Spring Boot 3.5.5
- MyBatis-Plus 3.5.17
- MySQL 8.4（Docker，现有 MySQL 8.x 容器也可复用）
- 包名统一使用 `com.wwweinuo`

## 项目结构

```text
spring-cloud-demo/
├── pom.xml                         # Maven 父工程，只负责聚合和统一版本
├── README.md                       # 项目说明、架构图和启动方式
├── .gitignore
├── common/                         # 公共响应结构和基础错误码
├── user-service/                   # 用户服务，端口 8081
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wwweinuo/cloudmall/user/
│       └── main/resources/application*.yml
├── product-service/                # 商品服务，端口 8082
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wwweinuo/cloudmall/product/
│       └── main/resources/application*.yml
├── order-service/                  # 订单服务，端口 8083
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wwweinuo/cloudmall/order/
│       └── main/resources/application*.yml
├── docker-compose.yml              # MySQL 容器配置，root/root
└── docker/mysql/init/01-schema.sql # 带详细注释的建表和演示数据脚本
```

架构关系：

```text
客户端
  ├──→ user-service     :8081 ──→ mall_user
  ├──→ product-service  :8082 ──→ mall_product
  └──→ order-service    :8083 ──→ mall_order / mall_order_item

                       MySQL
```

当前三个服务可以独立启动和编译，但订单服务暂不通过网络调用用户服务或商品服务。订单表中的 `user_id`、`product_id` 是逻辑引用；进入 OpenFeign 阶段后，再由订单服务远程获取用户和商品信息。

## 服务职责和接口

| 服务 | 端口 | 职责 | 接口 |
| --- | ---: | --- | --- |
| user-service | 8081 | 用户资料、状态和基础查询 | `GET /users/{id}` |
| product-service | 8082 | 商品信息、价格和上下架状态 | `GET /products/{id}` |
| order-service | 8083 | 订单创建、状态和查询 | `POST /orders`、`GET /orders/{id}` |

三个服务都提供 `GET /internal/info`，用于确认端口和进程没有串错。

## MySQL 和 MyBatis-Plus

启动项目自带 MySQL：

```bash
docker compose up -d mysql
docker compose ps
```

如果宿主机的 `3306` 已经有其他 MySQL 容器，可以改用 `3307`：

```powershell
$env:MYSQL_HOST_PORT = "3307"
docker compose up -d mysql
$env:MYSQL_PORT = "3307"
```

应用默认连接：

| 配置项 | 值 |
| --- | --- |
| 地址 | `localhost:3306` |
| 数据库 | `cloud_mall` |
| 用户名 | `root` |
| 密码 | `root` |

建表脚本位于 [`docker/mysql/init/01-schema.sql`](D:/code/spring-cloud-demo/docker/mysql/init/01-schema.sql)，包含 `mall_user`、`mall_product`、`mall_order` 和 `mall_order_item` 四张表，以及用户和商品各一条演示数据。

每个服务只依赖和操作自己的 Mapper。三个服务可以共用同一个 MySQL 实例，但业务表的所有权保持隔离，为后续拆分数据库保留空间。

配置文件按 Spring Boot profile 划分：`application.yml` 保存服务名和公共 MyBatis-Plus 配置，默认使用 `dev`；`application-dev.yml`、`application-test.yml` 和 `application-prod.yml` 分别保存开发、测试和生产差异。启动时可以通过 `--spring.profiles.active=test` 或 `--spring.profiles.active=prod` 切换环境。

## 构建和启动

全量构建：

```bash
mvn clean test
```

先打包，再分别运行：

```bash
mvn -pl user-service,product-service,order-service -am package -DskipTests
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
java -jar product-service/target/product-service-1.0.0-SNAPSHOT.jar
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
```

验证接口：

```bash
curl http://localhost:8081/internal/info
curl http://localhost:8082/internal/info
curl http://localhost:8083/internal/info
curl http://localhost:8081/users/1
curl http://localhost:8082/products/1
```

订单服务在尚未接入 OpenFeign 时，创建请求可以携带商品快照字段：

```bash
curl -X POST http://localhost:8083/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"quantity":2,"productName":"CloudMall 入门商品","unitPrice":99.00}'
```

## 后续演进

1. Nacos：服务注册、发现和统一配置。
2. OpenFeign：订单服务调用用户服务和商品服务。
3. Gateway：统一入口和路由转发。
4. Sentinel：超时、限流、熔断和降级。
5. Seata / MQ：处理跨服务数据一致性。
6. Trace / Prometheus：完善链路追踪、指标和日志观测。
