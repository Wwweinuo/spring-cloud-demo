-- ============================================================================
-- CloudMall 数据库初始化脚本
-- ============================================================================
-- 说明：
-- 1. 本脚本由 docker-compose.yml 挂载到 MySQL 容器中，在数据库数据目录
--    第一次初始化时自动执行。
-- 2. 脚本只负责表结构和少量演示数据，不包含生产环境的账号、权限和业务数据。
-- 3. 所有表统一使用 InnoDB、utf8mb4 和 Asia/Shanghai 时区约定。
-- 4. user/product/order 当前仍在同一个模块化单体中运行；表名按业务边界
--    命名，后续拆分微服务时可以分别迁移到各自的数据库。
-- ============================================================================

-- 允许直接使用 mysql 客户端执行本文件，而不依赖 MYSQL_DATABASE 环境变量。
CREATE DATABASE IF NOT EXISTS cloud_mall
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cloud_mall;

-- ----------------------------------------------------------------------------
-- 用户表：保存用户基础资料和账号状态。
-- 订单只保存 user_id 逻辑引用，不在数据库层建立跨业务外键，避免订单边界
-- 直接依赖用户表的物理约束；用户是否存在由应用服务在创建订单时校验。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mall_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键，MySQL 自增',
    username VARCHAR(64) NOT NULL COMMENT '登录名或用户展示名，当前要求唯一',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE 启用、DISABLED 禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0 未删除、1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_user_username (username),
    KEY idx_mall_user_status (status),
    KEY idx_mall_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='CloudMall 用户基础信息表';

-- ----------------------------------------------------------------------------
-- 商品表：保存商品主数据、价格和上下架状态。
-- 订单明细会冗余保存商品名称和成交单价，保证商品改名或调价后历史订单
-- 仍然能够展示当时的快照信息。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mall_product (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品主键，MySQL 自增',
    name VARCHAR(128) NOT NULL COMMENT '商品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '当前销售单价，单位：元',
    status VARCHAR(16) NOT NULL DEFAULT 'ON_SALE' COMMENT '商品状态：ON_SALE 在售、OFF_SALE 下架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0 未删除、1 已删除',
    PRIMARY KEY (id),
    KEY idx_mall_product_status (status),
    KEY idx_mall_product_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='CloudMall 商品主数据表';

-- ----------------------------------------------------------------------------
-- 订单主表：保存一次下单行为的总体信息。
-- order_no 是对外使用的业务订单号；id 只作为内部数据库主键。
-- total_amount 在创建订单时由商品价格和数量计算，避免查询商品当前价格时
-- 重新计算历史订单金额。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mall_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单主键，MySQL 自增',
    order_no VARCHAR(32) NOT NULL COMMENT '业务订单号，对外展示和幂等处理使用',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '下单用户 ID，逻辑关联 mall_user.id',
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额，单位：元',
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态：CREATED 已创建、PAID 已支付、CANCELLED 已取消',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0 未删除、1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_order_order_no (order_no),
    KEY idx_mall_order_user_id (user_id),
    KEY idx_mall_order_status (status),
    KEY idx_mall_order_created_at (created_at),
    KEY idx_mall_order_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='CloudMall 订单主表';

-- ----------------------------------------------------------------------------
-- 订单明细表：保存订单中的商品行和成交快照。
-- 当前示例接口一次只创建一条明细，但单独建表可以自然支持未来的多商品订单。
-- order_id 是订单边界内的物理外键；product_id 仍是逻辑引用，不跨业务建立外键。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mall_order_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单明细主键，MySQL 自增',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '所属订单 ID，关联 mall_order.id',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品 ID，逻辑关联 mall_product.id',
    product_name VARCHAR(128) NOT NULL COMMENT '下单时的商品名称快照',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '下单时的商品单价快照，单位：元',
    quantity INT UNSIGNED NOT NULL COMMENT '购买数量，必须大于 0',
    subtotal DECIMAL(12, 2) NOT NULL COMMENT '明细小计，等于 unit_price * quantity',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '明细创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '明细最后更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0 未删除、1 已删除',
    PRIMARY KEY (id),
    KEY idx_mall_order_item_order_id (order_id),
    KEY idx_mall_order_item_product_id (product_id),
    KEY idx_mall_order_item_deleted (deleted),
    CONSTRAINT fk_mall_order_item_order_id
        FOREIGN KEY (order_id) REFERENCES mall_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='CloudMall 订单商品明细表';

-- ----------------------------------------------------------------------------
-- 演示数据：让本地启动后可以直接访问 /users/1 和 /products/1。
-- INSERT IGNORE 保证脚本重复执行时不会因为唯一键冲突而中断。
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO mall_user (id, username, status)
VALUES (1, 'demo-user', 'ACTIVE');

INSERT IGNORE INTO mall_product (id, name, price, status)
VALUES (1, 'CloudMall 入门商品', 99.00, 'ON_SALE');
