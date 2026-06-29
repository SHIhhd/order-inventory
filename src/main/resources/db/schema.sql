CREATE DATABASE IF NOT EXISTS order_inventory
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE order_inventory;

DROP TABLE IF EXISTS stock_flow;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS order_info;
DROP TABLE IF EXISTS product_stock;
DROP TABLE IF EXISTS product;

CREATE TABLE product (
                         id BIGINT NOT NULL COMMENT '主键ID，MyBatis-Plus雪花算法生成',
                         sku_code VARCHAR(64) NOT NULL COMMENT '商品SKU编码，业务唯一标识',
                         product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
                         product_status TINYINT NOT NULL DEFAULT 0 COMMENT '商品状态：0-下架，1-上架',
                         sale_price BIGINT NOT NULL COMMENT '销售单价，单位：分',
                         remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
                         create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                         update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                         is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                         version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_product_sku_code (sku_code),
                         KEY idx_product_name (product_name),
                         KEY idx_product_status (product_status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='商品表：保存商品基础信息';

CREATE TABLE product_stock (
                               id BIGINT NOT NULL COMMENT '主键ID，MyBatis-Plus雪花算法生成',
                               product_id BIGINT NOT NULL COMMENT '商品ID，对应product.id',
                               available_quantity INT NOT NULL DEFAULT 0 COMMENT '可用库存数量',
                               locked_quantity INT NOT NULL DEFAULT 0 COMMENT '锁定库存数量，第一版可不启用，预留给后续库存预占',
                               total_in_quantity INT NOT NULL DEFAULT 0 COMMENT '累计入库数量',
                               total_out_quantity INT NOT NULL DEFAULT 0 COMMENT '累计出库数量',
                               remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
                               create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                               update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                               is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                               version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_product_stock_product_id (product_id),
                               KEY idx_product_stock_available_quantity (available_quantity)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='商品库存表：保存商品当前库存信息';

CREATE TABLE order_info (
                            id BIGINT NOT NULL COMMENT '主键ID，MyBatis-Plus雪花算法生成',
                            order_no VARCHAR(64) NOT NULL COMMENT '订单编号，业务唯一标识',
                            buyer_id BIGINT NOT NULL COMMENT '下单用户ID，第一版不建立用户表，仅保留扩展字段',
                            order_status TINYINT NOT NULL DEFAULT 10 COMMENT '订单状态：10-已创建，20-已取消，30-已完成',
                            total_amount BIGINT NOT NULL DEFAULT 0 COMMENT '订单总金额，单位：分',
                            total_quantity INT NOT NULL DEFAULT 0 COMMENT '订单商品总数量',
                            cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
                            remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
                            create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                            update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                            is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                            version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_order_info_order_no (order_no),
                            KEY idx_order_info_buyer_id (buyer_id),
                            KEY idx_order_info_status_create_time (order_status, create_time)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='订单主表：保存订单整体信息';

CREATE TABLE order_item (
                            id BIGINT NOT NULL COMMENT '主键ID，MyBatis-Plus雪花算法生成',
                            order_id BIGINT NOT NULL COMMENT '订单ID，对应order_info.id',
                            order_no VARCHAR(64) NOT NULL COMMENT '订单编号，对应order_info.order_no',
                            product_id BIGINT NOT NULL COMMENT '商品ID，对应product.id',
                            sku_code VARCHAR(64) NOT NULL COMMENT '商品SKU编码，下单时快照',
                            product_name VARCHAR(128) NOT NULL COMMENT '商品名称，下单时快照',
                            sale_price BIGINT NOT NULL COMMENT '销售单价，下单时快照，单位：分',
                            quantity INT NOT NULL COMMENT '购买数量',
                            item_amount BIGINT NOT NULL COMMENT '明细金额，单位：分，计算规则：sale_price * quantity',
                            create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                            update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                            is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                            version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_order_item_order_product (order_id, product_id),
                            KEY idx_order_item_order_no (order_no),
                            KEY idx_order_item_product_id (product_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='订单明细表：保存订单中的商品快照信息';

CREATE TABLE stock_flow (
                            id BIGINT NOT NULL COMMENT '主键ID，MyBatis-Plus雪花算法生成',
                            product_id BIGINT NOT NULL COMMENT '商品ID，对应product.id',
                            sku_code VARCHAR(64) NOT NULL COMMENT '商品SKU编码',
                            biz_no VARCHAR(64) NOT NULL COMMENT '业务单号，例如订单编号',
                            biz_type TINYINT NOT NULL COMMENT '业务类型：1-下单扣减库存，2-取消订单回滚库存，3-人工调整库存',
                            change_quantity INT NOT NULL COMMENT '库存变化数量：正数表示增加，负数表示扣减',
                            before_quantity INT NOT NULL COMMENT '变更前可用库存数量',
                            after_quantity INT NOT NULL COMMENT '变更后可用库存数量',
                            operator_id BIGINT DEFAULT NULL COMMENT '操作人ID，系统操作时可为空',
                            operator_type TINYINT NOT NULL DEFAULT 0 COMMENT '操作来源：0-系统，1-用户，2-管理员',
                            remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
                            create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                            update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                            is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                            version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_stock_flow_biz_product_type (biz_no, product_id, biz_type),
                            KEY idx_stock_flow_product_create_time (product_id, create_time),
                            KEY idx_stock_flow_biz_no (biz_no)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='库存流水表：记录每一次库存变化';