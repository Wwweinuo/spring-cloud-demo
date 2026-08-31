# 12-Docker 可复现部署与交付

> 本文是《Java 后端微服务学习计划》的优先级 12 专题，也是当前 CloudMall 主线的阶段性收尾专题。

## 一、本阶段学习目标

- 理解镜像、容器、网络、卷和 Compose 的职责。
- 将 Nacos、MySQL、Redis、MQ、Gateway 和业务服务统一启动。
- 使用环境变量注入环境差异配置。
- 为基础设施和服务增加健康检查。
- 处理依赖启动顺序、日志、端口和数据持久化。
- 让其他人可以按照 README 复现项目。

## 二、为什么需要容器化

手工启动多个服务时，容易出现：

- JDK、Maven 或中间件版本不一致。
- 端口和环境变量配置不一致。
- 启动顺序错误。
- 本地数据和测试数据混用。
- 只在某一台电脑上能运行。

Docker 把运行环境和启动方式写成配置，让项目从“我电脑能运行”变成“按文档可以复现”。容器化不能自动解决业务设计问题，服务边界、超时、幂等和故障恢复仍需要在代码和文档中明确。

## 三、核心对象

| 对象 | 作用 |
| --- | --- |
| Image | 可分发的应用运行模板 |
| Container | 镜像的运行实例 |
| Network | 容器之间的通信网络 |
| Volume | 持久化数据库或中间件数据 |
| Compose | 编排多个容器和依赖关系 |

容器之间应使用 Compose 服务名通信，例如 `mysql:3306`、`nacos:8848`，不要在容器内使用宿主机的 `localhost` 访问另一个容器。

## 四、环境变量和配置隔离

同一镜像在开发、测试和生产环境中应通过环境变量改变连接地址、端口和密钥：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:mysql}:${MYSQL_PORT:3306}/cloud_mall
```

注意：

- 密码和 Token 不写入镜像和 Git。
- `dev`、`test`、`prod` 使用不同 Namespace 和数据库。
- 环境变量名称应有统一前缀。
- 配置优先级和覆盖关系要记录到 README。

## 五、Compose 编排思路

目标依赖关系：

```text
mysql / nacos / redis / mq
          ↓ 健康
       Gateway
          ↓ 注册发现
业务服务
```

Compose 示例骨架：

```yaml
services:
  mysql:
    image: mysql:8.4
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]

  gateway:
    build: ./gateway
    depends_on:
      nacos:
        condition: service_started
    environment:
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: dev
    ports:
      - "8000:8000"
```

实际项目中应根据 Nacos、数据库和服务镜像的健康检查能力完善 `depends_on`。启动顺序只能减少问题，不能代替服务自身的连接重试和故障处理。

## 六、镜像构建注意事项

- 使用多阶段构建减少最终镜像体积。
- 运行阶段只保留 JRE 和应用 Jar 所需文件。
- 使用非 root 用户运行应用。
- 显式设置时区、字符集和 JVM 参数。
- 为容器配置合理的停止时间，让应用有机会优雅停机。
- 不把 `target`、日志、数据库数据打进镜像。

## 七、部署实验

### 实验 1：一键启动

```bash
docker compose up -d
docker compose ps
```

确认 Nacos、MySQL、Gateway 和业务服务状态。

### 实验 2：容器重启

重启商品服务容器，观察它是否重新注册到 Nacos，Gateway 是否能继续通过服务名访问。

### 实验 3：依赖不可用

停止 Redis 或 MQ，观察依赖服务的健康状态、日志和业务降级，不要只看容器是否仍然运行。

### 实验 4：数据持久化

重建 MySQL 容器，确认 Volume 和初始化脚本是否符合预期，并验证开发数据不会意外覆盖其他环境。

## 八、交付检查清单

- [ ] 新机器可以按 README 启动基础设施。
- [ ] 容器之间使用服务名通信。
- [ ] 配置通过环境变量或配置中心注入。
- [ ] 端口、Namespace、Group 和 Data ID 已记录。
- [ ] 服务有健康检查和可查看日志。
- [ ] MySQL、Redis、MQ 数据有明确的持久化策略。
- [ ] 依赖不可用时有可理解的错误和恢复方式。
- [ ] 故障实验和预期结果已写入项目文档。

## 九、最终能力验收

```text
客户端
  ↓
Gateway
  ↓
Nacos 服务发现 / LoadBalancer
  ↓
业务服务
  ├── MySQL
  ├── Redis
  └── MQ
        ↓
日志 / 指标 / Trace
```

完成本阶段后，应能在不依赖个人机器状态的情况下启动项目，访问主要接口，查看服务状态，定位一次故障，并解释配置、网络、数据和消息在系统中的位置。
