# 3-OpenFeign 服务间远程调用

> 本文是《Java 后端微服务学习计划》的优先级 3 专题。
>
> 前置条件：已经完成微服务基础拆分和 Nacos 服务注册。当前 CloudMall 已具备 `api` 模块、用户服务、商品服务和订单服务，可以直接用于本阶段实验。

## 一、本阶段学习目标

完成本阶段后，应该能够：

- 解释服务间方法调用和 HTTP 远程调用的区别。
- 使用 `@FeignClient` 按服务名调用下游服务。
- 使用独立的 API 模块维护调用契约和 DTO。
- 处理远程服务不存在、超时、异常响应和空数据。
- 区分连接超时、读取超时、业务错误和网络错误。
- 说明重试为什么可能放大流量并造成重复写入。

## 二、为什么需要 OpenFeign

单体应用中，订单模块可以直接调用商品模块的方法：

```text
orderService.create()
  └── productService.getById()
```

拆成微服务后，订单服务和商品服务运行在不同进程中，调用变成：

```text
order-service
  └── HTTP 请求 → product-service
                     └── Controller → Service → Mapper
```

调用方需要处理服务地址、HTTP 方法、请求参数、响应序列化、超时和异常。OpenFeign 用接口声明的方式隐藏了部分 HTTP 客户端细节，但不会消除网络调用本身的不可靠性。

## 三、核心概念

### 3.1 `@FeignClient` 的服务名

```java
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductDTO getProduct(@PathVariable Long id);
}
```

`product-service` 来自商品服务的 `spring.application.name`，并且也是它注册到 Nacos 的服务名。Feign 不应该在接口中写死 `http://localhost:8082`。

### 3.2 API 模块和 DTO 边界

`api` 模块只维护跨服务契约：

```text
api
├── UserClient
├── ProductClient
├── UserDTO
└── ProductDTO
```

业务实体仍然属于各自服务。订单服务不能直接引入商品服务的 `Product` 实体，因为实体包含数据库结构和服务内部规则，复用实体会让服务边界重新耦合。

### 3.3 远程调用的四类结果

| 类型 | 示例 | 调用方处理方式 |
| --- | --- | --- |
| HTTP 成功且有数据 | 商品存在 | 继续业务流程 |
| HTTP 成功但数据为空 | 商品不存在或响应缺字段 | 转换为明确业务错误 |
| HTTP 业务失败 | 下游返回 4xx/5xx | 记录原因并返回可理解错误 |
| 网络失败 | 连接超时、读取超时、服务下线 | 快速失败、降级或稍后重试 |

## 四、CloudMall 实现方式

当前订单服务在启动类中显式启用 API 模块里的客户端：

```java
@EnableFeignClients(clients = {UserClient.class, ProductClient.class})
```

实现类通过构造器注入客户端：

```java
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements OrderService {

    private final UserClient userClient;
    private final ProductClient productClient;
}
```

订单创建流程为：

```text
创建订单请求
  ↓
订单服务调用 UserClient
  ↓
校验用户状态
  ↓
订单服务调用 ProductClient
  ↓
读取商品名称、价格和上下架状态
  ↓
写入订单自己的数据库
```

当前 Feign 超时配置位于订单服务配置中：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: ${FEIGN_CONNECT_TIMEOUT:2000}
            read-timeout: ${FEIGN_READ_TIMEOUT:3000}
```

连接超时表示建立连接花费太久；读取超时表示连接建立后等待响应数据花费太久。两者都应设置上限，不能无限等待。

## 五、故障实验

### 实验 1：商品服务下线

1. 保持 Nacos 和订单服务运行。
2. 停止 `product-service`。
3. 调用订单创建接口。
4. 观察订单服务日志和返回结果。

预期：订单服务不能继续依赖商品信息完成创建，应返回明确的下游不可用提示，不应出现无限等待。

### 实验 2：商品服务慢响应

在商品服务查询接口中临时增加超过 3 秒的延迟，再创建订单。观察读取超时是否生效，并确认订单服务进程没有被请求永久占用。

### 实验 3：HTTP 200 但数据为空

查询不存在的商品 ID，确认订单服务不会把空 DTO 当成有效商品继续写入订单。

### 实验 4：重试风险

让一次写请求在服务端已经落库后、响应返回前发生超时。分析调用方重试后是否可能产生重复订单，并说明为什么写请求不能默认无限重试。

## 六、通过标准

- [ ] 能解释 `@FeignClient(name = "product-service")` 中服务名的来源。
- [ ] 能从订单请求追踪到 Nacos、LoadBalancer、Feign 和商品服务。
- [ ] 能区分连接超时和读取超时。
- [ ] 能处理下游不可用、HTTP 错误和空数据。
- [ ] 能说明 DTO 与业务实体为什么必须分离。
- [ ] 能解释重试可能导致的流量放大和重复写入。

## 七、面试和自测问题

1. Feign 是不是直接调用了一个 Java 方法？
2. Feign 如何知道商品服务的 IP 和端口？
3. Nacos 不可用时，已经缓存的实例还能否继续调用？
4. 为什么 `api` 模块可以放 DTO，但不能放商品数据库实体？
5. HTTP 200 但 `data` 为空时，调用方应该如何判断？
6. 什么场景适合重试，什么场景不应该重试？

## 八、下一阶段衔接

Feign 解决了“如何声明远程调用”，但服务发现可能返回多个实例。下一阶段使用 LoadBalancer 研究“具体选择哪个实例，以及多个实例如何分流”。
