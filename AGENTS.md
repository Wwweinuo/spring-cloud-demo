# CloudMall 项目开发规范

## 基本技术约束

- 使用 JDK 25 和 Maven 3.9+。
- 使用 Spring Boot 3.5.x、Spring Cloud 2025.0.x 和 Spring Cloud Alibaba 2025.0.x。
- 使用 MyBatis-Plus 作为 ORM 框架，数据库使用 Docker 中的 MySQL。
- 项目包名统一使用 `com.wwweinuo`。
- 所有服务的配置按 Spring Profile 管理，开发环境默认使用 `dev`。

## 模块和职责边界

- 根项目只负责 Maven 聚合、依赖版本和公共构建配置。
- `common` 只放与具体业务无关的公共响应结构、错误码和基础工具，不放业务实体。
- `api` 是唯一的跨服务调用契约模块，集中维护 Feign Client 接口和跨服务传输 DTO。
- 各业务服务只维护自己的实体、Mapper、Service 和 Controller，不直接访问其他服务的数据表。
- 业务服务之间通过 Feign Client 和服务名调用，不在代码中写死 IP 和端口。
- Gateway 只负责路由、转发和统一网关治理，不编写订单、商品、用户等领域业务逻辑。

## Java 和 Lombok 规范

- 实体类禁止使用 `@Data`，优先使用 `@Getter`、`@Setter` 和必要的构造器注解。
- Spring Bean 注入优先使用 Lombok 的 `@RequiredArgsConstructor`。
- 被注入的依赖必须声明为 `private final`。
- 不手动编写仅用于依赖注入的构造方法。
- 不使用字段注入和 `@Autowired`，除非框架特殊场景确实无法使用构造器注入。

推荐写法：

```java
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
}
```

## Service 分层规范

- 每个业务服务的业务逻辑必须通过 Service 接口暴露。
- Service 接口放在 `service` 包，实现类放在 `service.impl` 包。
- 实现类继承 MyBatis-Plus 的 `ServiceImpl` 时，仍然必须实现本服务的 Service 接口。
- 实现类中调用其他 Bean 时使用 `@RequiredArgsConstructor` 和 `private final`。
- Controller 只负责参数接收、调用 Service 和返回统一响应，不直接操作 Mapper。

## 跨服务模型规范

- 不要在多个服务之间复用对方的数据库实体类。
- 跨服务请求和响应使用 `api` 模块中的 DTO 或 Client 契约。
- 服务内部实体只属于定义它的服务；即使字段相同，也不因为方便而共用实体。
- Feign Client 使用 Nacos 注册的服务名，例如 `product-service`。

## Nacos、Gateway 和调用规范

- 服务注册和配置中心默认使用 Nacos 的 `dev` Namespace 与 `DEFAULT_GROUP`。
- Data ID 按“服务名-环境名.yaml”命名，例如 `product-service-dev.yaml`。
- Gateway 使用 `lb://服务名` 作为路由 URI，通过 Nacos 服务发现和 LoadBalancer 选择实例。
- Gateway 当前使用 8000 端口；业务服务端口由各自配置文件维护。
- Gateway 路由通过 `StripPrefix` 处理统一入口前缀，不能把网关路径直接当作业务接口路径。
- Feign 调用必须设置合理的连接超时和读取超时，不对所有失败请求进行无限重试。
- 远程调用必须处理服务不存在、超时、异常响应和 HTTP 200 但数据为空等情况。

## 数据库规范

- 每个服务只操作自己拥有的表。
- 表结构变更必须同步维护 SQL 文件，并为数据库、表、字段、索引和约束添加清晰注释。
- 跨服务关联使用业务 ID 或逻辑引用，不建立跨服务数据库外键依赖。

## 验证和提交规范

- 修改后至少执行受影响模块的 Maven 测试；涉及根 POM 或公共模块时执行 `mvn test`。
- 新增服务或基础设施模块时，必须补充启动说明和可验证的接口命令到 README。
- 提交前检查 `git diff --check` 和 `git status --short`。
- 提交信息使用 Conventional Commit 格式，并准确说明本次变更的目的。
