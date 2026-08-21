# CloudMall 下一阶段学习计划

## 一、当前阶段判断

根据当前项目的学习计划、架构说明和 README，优先级 1 已经基本完成：

- 已创建 Maven 多模块工程。
- 已拆分 `common`、`user-service`、`product-service` 和 `order-service`。
- 三个业务服务拥有独立启动类、端口、Controller、Service、Mapper 和配置文件。
- 三个服务可以独立编译和启动。
- 已配置 `dev`、`test` 和 `prod` 环境配置文件。
- 已理解服务拆分后为什么需要服务发现和统一配置。
- 已理解固定服务地址的局限。
- 已理解 `spring.application.name` 的作用。

因此，下一阶段进入学习计划中的：

> 优先级 2：Nacos 注册中心与配置中心

本阶段的核心问题是：

> 服务之间如何通过服务名找到彼此？多套环境配置如何统一管理？

## 二、本阶段目标

完成本阶段后，应当能够：

- 解释服务注册和服务发现的基本过程。
- 解释配置中心和本地配置文件的区别。
- 让三个业务服务注册到 Nacos。
- 在 Nacos 控制台查看服务实例和健康状态。
- 使用 Namespace 区分开发、测试和生产环境。
- 让服务从 Nacos 读取一部分配置。
- 修改配置并验证配置刷新效果。
- 分析 Nacos 不可用、配置不存在和服务实例下线等故障。

## 三、第一部分：理解 Nacos 解决的问题

先掌握以下概念：

- 服务注册。
- 服务发现。
- 服务实例。
- 健康检查。
- Namespace。
- Group。
- Data ID。
- 配置中心。
- 配置动态刷新。

需要能够回答：

1. 服务为什么不能长期固定写死 IP 和端口？
2. 注册中心和配置中心分别解决什么问题？
3. `spring.application.name` 如何参与服务注册和配置定位？
4. Namespace、Group、Data ID 分别有什么作用？

## 四、第二部分：将 Nacos 加入项目

在当前项目中增加 Nacos 基础设施，整体结构如下：

```text
CloudMall
├── Nacos
├── MySQL
├── user-service
├── product-service
└── order-service
```

建议先通过 Docker 启动 Nacos，并在 README 中补充启动命令。

本阶段先只接入服务注册和配置中心，不要同时引入 Gateway、OpenFeign、Sentinel、Seata 或 MQ。

开始编码前，需要先确认 Spring Boot、Spring Cloud 和 Spring Cloud Alibaba Nacos 的版本兼容关系，并统一配置到根 `pom.xml`。

## 五、第三部分：让三个服务完成注册

为三个服务增加 Nacos Discovery 相关依赖，使它们能够注册到 Nacos：

```text
user-service
product-service
order-service
        ↓
      Nacos
```

验证内容：

- 三个服务是否都出现在 Nacos 控制台。
- 服务名是否正确。
- 服务端口是否正确。
- 服务停止后实例是否下线或被标记为不健康。
- 服务重启后实例是否重新注册。

预期注册结果：

```text
user-service    8081
product-service 8082
order-service   8083
```

这一阶段暂时不要求订单服务通过 Nacos 调用其他服务，先验证注册中心本身工作正常。

## 六、第四部分：接入 Nacos 配置中心

当前本地配置文件结构可以继续保留，先逐步把环境差异配置迁移到 Nacos，避免一次性改动过大。

建议的配置命名方式：

```text
user-service-dev.yml
user-service-test.yml
user-service-prod.yml

product-service-dev.yml
product-service-test.yml
product-service-prod.yml

order-service-dev.yml
order-service-test.yml
order-service-prod.yml
```

先迁移少量配置，验证配置加载链路：

```yaml
server:
  port: 8081

app:
  message: user-service-dev
```

确认服务能够正常读取后，再逐步迁移：

- 数据库连接。
- Redis 配置。
- MQ 配置。
- 第三方服务地址。
- 日志级别。

不要一开始就把所有配置全部迁移。优先验证配置中心的加载、覆盖和刷新机制。

## 七、第五部分：设计环境隔离

建议使用 Namespace 区分环境：

```text
dev
test
prod
```

示例配置：

```text
Namespace: dev
Group: DEFAULT_GROUP
Data ID: user-service-dev.yml
```

测试环境和生产环境必须使用不同 Namespace，避免服务误读取其他环境的数据库、Redis 或 MQ 配置。

需要重点理解：

- Namespace 主要用于环境隔离。
- Group 用于对配置或服务进行逻辑分组。
- Data ID 用于定位具体配置文件。
- 服务名通常参与服务注册和配置定位。

## 八、第六部分：配置刷新实验

选择一个简单配置进行动态刷新，例如：

```yaml
app:
  message: hello-dev
```

完成以下实验：

1. 服务启动时读取配置。
2. 修改 Nacos 中的配置值。
3. 验证服务是否能获取新值。
4. 如果不能刷新，检查是否缺少刷新相关配置或注解。
5. 对比重启加载和动态刷新两种方式的区别。

动态刷新只用于适合运行期变化的配置。数据库地址、密钥和核心基础设施配置不能为了追求刷新而随意修改。

## 九、第七部分：故障实验

至少完成以下实验，并把结果补充到 [`故障实验记录.md`](故障实验记录.md)：

### 实验 1：Nacos 未启动

- 服务是否能够启动？
- 服务是否能够读取本地配置？
- 服务注册失败后是否会持续重试？

### 实验 2：Data ID 配置错误

- 服务启动时会出现什么错误？
- 是直接启动失败，还是使用本地配置？
- 哪种行为更适合生产环境？

### 实验 3：Namespace 配置错误

- 服务能否读取配置？
- 服务注册到了哪个环境？
- 如何通过日志和 Nacos 控制台定位问题？

### 实验 4：服务实例下线

- 停止一个服务实例后，Nacos 控制台有什么变化？
- 后续调用是否还能找到该实例？
- 健康检查和实例剔除分别解决什么问题？

### 实验 5：配置动态刷新

- 修改配置后，服务是否获取了新值？
- 哪些 Bean 会重新创建？
- 动态刷新可能带来哪些风险？

## 十、本阶段项目改造顺序

建议严格按照以下顺序实施：

1. 启动 Nacos，并确认控制台可以访问。
2. 统一根 `pom.xml` 中的 Nacos 依赖版本。
3. 让 `user-service` 注册成功。
4. 让 `product-service` 和 `order-service` 注册成功。
5. 验证服务名称、端口和健康状态。
6. 为 `user-service` 增加一份最小 Nacos 配置。
7. 验证配置读取和配置覆盖。
8. 将相同方案应用到商品服务和订单服务。
9. 配置 dev、test、prod Namespace。
10. 完成 Nacos 故障实验。
11. 更新 README 和故障实验记录。

## 十一、本阶段完成标准

满足以下条件后，再进入 OpenFeign：

- [ ] 三个服务都能注册到 Nacos。
- [ ] 能在 Nacos 控制台查看服务实例。
- [ ] 能区分服务注册和配置中心的作用。
- [ ] 能使用 Namespace 隔离 dev、test、prod。
- [ ] 至少一个服务能够从 Nacos 读取配置。
- [ ] 能修改配置并验证刷新结果。
- [ ] 能处理 Nacos 不可用、配置不存在和实例下线等情况。
- [ ] README 已补充 Nacos 的启动和配置说明。
- [ ] 故障实验结果已记录到项目文档。

## 十二、下一阶段衔接

完成 Nacos 后，进入学习计划中的：

> 优先级 3：OpenFeign 服务间远程调用

届时让 `order-service` 通过服务名调用：

```text
order-service
    ├──→ user-service
    └──→ product-service
```

OpenFeign 阶段重点解决远程调用、DTO 边界、超时、异常处理和下游服务不可用问题。

