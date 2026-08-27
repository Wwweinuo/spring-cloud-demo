# spring-cloud-demo

CloudMall 当前按照推荐的微服务多模块结构组织。三个业务服务拥有独立的启动类、端口、配置、Controller、Service、Mapper 和数据边界；当前已接入 Nacos 服务注册和配置中心，暂不接入 OpenFeign、Gateway、Sentinel、Seata 和 MQ。

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

三个服务 ──→ Nacos :8848
业务数据 ──→ MySQL
```

当前三个服务可以独立启动和编译，启动后会向 Nacos 注册自身的服务名、地址和端口，但订单服务暂不通过网络调用用户服务或商品服务。订单表中的 `user_id`、`product_id` 是逻辑引用；进入 OpenFeign 阶段后，再由订单服务通过 Nacos 服务发现远程获取用户和商品信息。

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

## Nacos 学习环境

当前阶段使用 Docker 启动 Nacos 单机版，作为后续学习服务注册、服务发现和配置中心的基础环境。Nacos 使用内置 Derby 存储，适合本地学习；如果删除容器，Nacos 中保存的服务和配置数据也会丢失。

在 PowerShell 中执行以下单行命令：

```powershell
docker run --name nacos-standalone --restart unless-stopped -e MODE=standalone -e NACOS_AUTH_TOKEN=c3ByaW5nLWNsb3VkLWRlbW8tbmFjb3Mtc2VjcmV0LXRva2VuLTIwMjY= -e NACOS_AUTH_IDENTITY_KEY=nacos -e NACOS_AUTH_IDENTITY_VALUE=nacos -p 8080:8080 -p 8848:8848 -p 9848:9848 -d nacos/nacos-server:latest
```

启动后访问 Nacos 控制台：<http://localhost:8080>。首次进入时，按照页面提示初始化 `nacos` 管理员密码。

查看启动日志：

```powershell
docker logs -f nacos-standalone
```

如果容器已经创建过，不要重复执行 `docker run`，直接启动已有容器：

```powershell
docker start nacos-standalone
```

Nacos 端口说明：`8080` 用于控制台，`8848` 用于客户端和 HTTP API，`9848` 用于客户端 gRPC 通信。三个 Spring Boot 服务接入 Nacos 时，服务地址使用 `localhost:8848`，而不是 `localhost:8080`。

### Nacos 配置中心

三个服务都已通过 `spring.config.import` 接入 Nacos 配置中心，并根据当前 Spring profile 导入对应配置。配置默认使用 `dev` 命名空间和 `DEFAULT_GROUP` 分组，也可以通过 `NACOS_NAMESPACE` 环境变量覆盖，Data ID 如下：

| 服务 | dev | test | prod |
| --- | --- | --- | --- |
| user-service | `user-service-dev.yaml` | `user-service-test.yaml` | `user-service-prod.yaml` |
| product-service | `product-service-dev.yaml` | `product-service-test.yaml` | `product-service-prod.yaml` |
| order-service | `order-service-dev.yaml` | `order-service-test.yaml` | `order-service-prod.yaml` |

当前先为 `user-service` 创建一份最小配置，用于验证配置中心加载链路：

```text
Namespace: dev
Group: DEFAULT_GROUP
Data ID: user-service-dev.yaml
```

配置内容：

```yaml
cloudmall:
  greeting: hello from nacos
```

三个服务都通过 `spring.config.import` 导入上表中的配置，并开启配置变更监听。配置不存在时使用 `optional:nacos:`，不会阻止应用启动；发布配置后重启对应服务即可先验证配置是否被加载。正式迁移业务配置时，建议保持 Data ID 与服务名、环境名一致。

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

1. Nacos 配置中心：将三个服务的业务配置逐步迁移到 Nacos，并验证动态刷新。
2. OpenFeign：订单服务调用用户服务和商品服务。
3. Gateway：统一入口和路由转发。
4. Sentinel：超时、限流、熔断和降级。
5. Seata / MQ：处理跨服务数据一致性。
6. Trace / Prometheus：完善链路追踪、指标和日志观测。
