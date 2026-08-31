# 5-Gateway 统一入口与路由治理

> 本文是《Java 后端微服务学习计划》的优先级 5 专题。
>
> CloudMall 当前已经新增 `gateway` 模块，默认端口为 `8000`，可直接按本文进行实验。

## 一、本阶段学习目标

- 理解客户端直接访问服务带来的问题。
- 掌握 Route、Predicate、Filter 和 GlobalFilter 的职责。
- 使用 `lb://服务名` 完成基于服务发现的路由。
- 理解 `StripPrefix` 和请求路径转换。
- 认识 Gateway 的边界：统一治理，不承载领域业务。
- 能分析网关下线、路由不存在和下游不可用等故障。

## 二、为什么需要统一入口

没有网关时，客户端需要知道所有业务服务地址：

```text
客户端 ──→ user-service:8081
客户端 ──→ product-service:8082
客户端 ──→ order-service:8083
```

这样会暴露内部服务结构，客户端也需要维护多套地址和认证规则。引入 Gateway 后：

```text
客户端 ──→ Gateway:8000
              ├──→ user-service
              ├──→ product-service
              └──→ order-service
```

Gateway 适合处理路由、认证前置、跨域、请求日志、限流和统一响应头；用户、商品、订单的业务规则仍然属于各自服务。

## 三、四个核心概念

| 概念 | 作用 | CloudMall 示例 |
| --- | --- | --- |
| Route | 定义完整转发规则 | `user-service` 路由 |
| Predicate | 判断请求是否匹配 | `Path=/api/users/**` |
| Filter | 修改请求或响应 | `StripPrefix=1` |
| GlobalFilter | 对所有路由统一处理 | 统一 TraceId、日志 |

匹配和转发的大致顺序是：

```text
请求进入
  ↓
匹配 Route Predicate
  ↓
执行请求过滤器
  ↓
通过 LoadBalancer 选择实例
  ↓
转发到下游服务
  ↓
执行响应过滤器并返回
```

## 四、CloudMall 当前路由

当前配置位于 `gateway/src/main/resources/application.yml`：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: product-service
              uri: lb://product-service
              predicates:
                - Path=/api/products/**
              filters:
                - StripPrefix=1
```

三条路由的映射关系：

| 客户端路径 | 目标服务 | 下游路径 |
| --- | --- | --- |
| `/api/users/1` | `user-service` | `/users/1` |
| `/api/products/1` | `product-service` | `/products/1` |
| `/api/orders` | `order-service` | `/orders` |

`StripPrefix=1` 表示去掉路径的第一段 `/api`。如果不去掉，商品服务会收到 `/api/products/1`，而当前业务接口是 `/products/1`。

## 五、路由实验

### 实验 1：网关健康检查

```bash
curl http://localhost:8000/actuator/health
```

### 实验 2：三条路由转发

```bash
curl http://localhost:8000/api/users/1
curl http://localhost:8000/api/products/1
curl -X POST http://localhost:8000/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"quantity":2}'
```

实测结果：健康检查返回 `{"status":"UP"}`，用户、商品和订单三条路由均成功转发。订单创建返回 HTTP 200，业务响应中的 `code` 为 `0`，订单状态为 `CREATED`，说明请求已经经过 Gateway 到达订单服务，并完成了订单服务对用户和商品服务的调用。

### 实验 3：路由和下游故障

- 修改路径为 `/api/unknown/1`，观察没有匹配路由时的结果。
- 停止商品服务，访问 `/api/products/1`，观察 Gateway 如何表现下游不可用。
- 让商品服务启动两个实例，观察 Gateway 是否通过 LoadBalancer 分流。

实测记录（2026-08-31）：

1. 未匹配路由 `/api/unknown/1` 返回 HTTP 404，错误为 `Not Found`。这说明请求没有匹配任何 Route，未进入下游服务。
2. 停止商品服务后访问 `/api/products/1` 返回 HTTP 500，错误为 `Internal Server Error`。这说明 Gateway 已匹配商品路由，但 LoadBalancer 找不到可用的商品服务实例或转发失败。
3. 商品服务启动两个同名实例后，连续访问 `/api/products/1`，通过两个实例的端口日志确认请求发生了分流，说明 Gateway、Nacos 服务发现和 LoadBalancer 已形成完整调用链。

当前下游不可用场景返回的是 Gateway 默认 500 响应，后续可以增加统一异常处理，将依赖不可用映射为更明确的 HTTP 503，并补充错误码、TraceId 和用户提示。

## 六、Gateway 的职责边界

不应在 Gateway 中实现：

- 创建订单和扣减库存。
- 判断商品是否允许购买。
- 修改订单状态。
- 访问业务数据库。
- 复用订单服务的业务实体。

Gateway 只知道“请求应该到哪里”和“入口请求是否满足公共规则”，不应该知道订单领域的完整业务流程。否则网关会变成所有业务的超级服务，任何业务变化都需要发布网关。

## 七、Nacos 配置中心迁移

当前路由保存在本地配置，`application-dev.yml` 预留了：

```yaml
spring:
  config:
    import:
      - optional:nacos:gateway-dev.yaml?group=DEFAULT_GROUP&refreshEnabled=true
```

后续可在 `dev` Namespace 和 `DEFAULT_GROUP` 中创建 `gateway-dev.yaml`，把 `spring.cloud.gateway.server.webflux.routes` 迁移到 Nacos，再验证路由刷新。路由迁移时必须保持 Data ID、Namespace、Group 和配置前缀一致。

## 八、通过标准

- [ ] 能解释 Gateway、Route、Predicate、Filter 的职责。
- [ ] 能从 `/api/products/1` 追踪到商品服务的 `/products/1`。
- [ ] 能解释 `StripPrefix=1` 的作用。
- [ ] 能验证 Gateway 使用 Nacos 和 LoadBalancer 找到下游实例。
- [ ] 能说明 Gateway 挂掉和业务服务挂掉的区别。
- [ ] 能说明为什么网关不应该承载订单业务逻辑。

## 九、下一阶段衔接

Gateway 统一了入口，但还不能证明请求是谁发起的。下一阶段在网关加入 JWT 认证，并将可信用户上下文传递给下游服务。
