# 小型订单库存管理系统 - 第一版接口文档

> 版本：v1.0  
> 项目类型：Spring Boot 单体后端项目  
> 技术栈：Spring Boot、Spring MVC、MyBatis、MyBatis-Plus、MySQL  
> 适用阶段：第一版核心业务闭环  
> 接口基础路径：`/api/v1`

---

## 1. 文档说明

本文档用于指导“小型订单库存管理系统”第一版接口开发。

第一版不包含登录、权限、Redis、MQ、支付、秒杀等扩展能力，只聚焦以下核心链路：

```text
商品管理
→ 库存初始化
→ 创建订单
→ 扣减库存
→ 保存订单明细
→ 记录库存流水
→ 取消订单
→ 回滚库存
```

第一版重点体现：

```text
1. Controller 参数接收
2. DTO 参数校验
3. Service 业务规则处理
4. Mapper 数据访问
5. MyBatis-Plus 基础 CRUD
6. MyBatis XML 复杂查询
7. Spring 事务控制
8. 统一异常处理
9. 统一返回结构
10. 逻辑删除与乐观锁
```

---

## 2.系统结构

### 2.1项目结构：

```
order-inventory
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── orderinventory
│   │   │               ├── OrderInventoryApplication.java
│   │   │               │
│   │   │               ├── common
│   │   │               │   ├── domain
│   │   │               │   │   └── BaseEntity.java
│   │   │               │   ├── result
│   │   │               │   │   ├── ApiResult.java
│   │   │               │   │   └── ErrorCode.java
│   │   │               │   ├── exception
│   │   │               │   │   ├── BusinessException.java
│   │   │               │   │   └── GlobalExceptionHandler.java
│   │   │               │   ├── config
│   │   │               │   │   └── MybatisPlusConfig.java
│   │   │               │   └── handler
│   │   │               │       └── MybatisPlusMetaObjectHandler.java
│   │   │               │
│   │   │               ├── product
│   │   │               │   ├── controller
│   │   │               │   │   └── ProductController.java
│   │   │               │   ├── service
│   │   │               │   │   ├── ProductService.java
│   │   │               │   │   └── impl
│   │   │               │   │       └── ProductServiceImpl.java
│   │   │               │   ├── mapper
│   │   │               │   │   └── ProductMapper.java
│   │   │               │   ├── entity
│   │   │               │   │   └── Product.java
│   │   │               │   ├── dto
│   │   │               │   │   ├── ProductCreateRequest.java
│   │   │               │   │   ├── ProductUpdateRequest.java
│   │   │               │   │   └── ProductPageQuery.java
│   │   │               │   └── vo
│   │   │               │       └── ProductVO.java
│   │   │               │
│   │   │               ├── stock
│   │   │               │   ├── controller
│   │   │               │   │   └── StockController.java
│   │   │               │   ├── service
│   │   │               │   │   ├── StockService.java
│   │   │               │   │   └── impl
│   │   │               │   │       └── StockServiceImpl.java
│   │   │               │   ├── mapper
│   │   │               │   │   ├── ProductStockMapper.java
│   │   │               │   │   └── StockFlowMapper.java
│   │   │               │   ├── entity
│   │   │               │   │   ├── ProductStock.java
│   │   │               │   │   └── StockFlow.java
│   │   │               │   ├── dto
│   │   │               │   │   ├── StockAdjustRequest.java
│   │   │               │   │   └── StockFlowPageQuery.java
│   │   │               │   └── vo
│   │   │               │       ├── ProductStockVO.java
│   │   │               │       └── StockFlowVO.java
│   │   │               │
│   │   │               └── order
│   │   │                   ├── controller
│   │   │                   │   └── OrderController.java
│   │   │                   ├── service
│   │   │                   │   ├── OrderService.java
│   │   │                   │   └── impl
│   │   │                   │       └── OrderServiceImpl.java
│   │   │                   ├── mapper
│   │   │                   │   ├── OrderInfoMapper.java
│   │   │                   │   └── OrderItemMapper.java
│   │   │                   ├── entity
│   │   │                   │   ├── OrderInfo.java
│   │   │                   │   └── OrderItem.java
│   │   │                   ├── dto
│   │   │                   │   ├── OrderCreateRequest.java
│   │   │                   │   ├── OrderCreateItemRequest.java
│   │   │                   │   ├── OrderCancelRequest.java
│   │   │                   │   └── OrderPageQuery.java
│   │   │                   └── vo
│   │   │                       ├── OrderDetailVO.java
│   │   │                       ├── OrderItemVO.java
│   │   │                       └── OrderPageVO.java
│   │   │
│   │   └── resources
│   │       ├── application.yml
│   │       ├── db
│   │       │   └── schema.sql
│   │       └── mapper
│   │           ├── product
│   │           │   └── ProductMapper.xml
│   │           ├── stock
│   │           │   ├── ProductStockMapper.xml
│   │           │   └── StockFlowMapper.xml
│   │           └── order
│   │               ├── OrderInfoMapper.xml
│   │               └── OrderItemMapper.xml
│   │
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── orderinventory
│                       └── OrderInventoryApplicationTests.java
```

### 2.2 分包设计说明

#### `common`

放通用能力：

```
统一返回
统一异常
错误码
基础实体类
MyBatis-Plus 配置
自动填充处理器
```

不要把业务逻辑放进 `common`。

#### `product`

负责商品基础信息：

```
新增商品
修改商品
商品上下架
商品分页查询
商品详情查询
```

#### `stock`

负责库存：

```
初始化库存
人工调整库存
下单扣减库存
取消订单回滚库存
库存流水查询
```

#### `order`

负责订单：

```
创建订单
取消订单
订单详情
订单分页查询
```

## 2.配置文件

### 2.1、推荐配置文件结构

建议你先这样放：

```
src/main/resources
├── application.yml
├── application-dev.yml
├── db
│   └── schema.sql
└── mapper
    ├── product
    │   └── ProductMapper.xml
    ├── stock
    │   ├── ProductStockMapper.xml
    │   └── StockFlowMapper.xml
    └── order
        ├── OrderInfoMapper.xml
        └── OrderItemMapper.xml
```

第一版先使用：

```
application.yml
application-dev.yml
```

不要一开始就搞很多环境。

------

### 2.2、`application.yml`

这个文件放公共配置，不放数据库密码。

```
spring:
  application:
    name: order-inventory

  profiles:
    active: dev

server:
  port: 8080

  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml

  type-aliases-package: com.example.orderinventory.*.entity

  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    banner: false
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    root: info
    com.example.orderinventory: debug
    com.example.orderinventory.product.mapper: debug
    com.example.orderinventory.stock.mapper: debug
    com.example.orderinventory.order.mapper: debug
```

------

### 2.3、`application-dev.yml`

这个文件放本地开发环境配置。

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_inventory?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      pool-name: OrderInventoryHikariPool
      minimum-idle: 5
      maximum-pool-size: 10
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
```

你本地只需要改这里：

```
username: root
password: your_password
```

如果你的 MySQL 端口不是 `3306`，再改这里：

```
localhost:3306
```

------

### 2.4、每一段配置解释

#### 2.4.1. 项目名称

```
spring:
  application:
    name: order-inventory
```

作用：给当前 Spring Boot 应用起名字。
 第一版虽然不用注册中心，但保留这个配置是好习惯。

------

#### 2.4.2. 激活开发环境

```
spring:
  profiles:
    active: dev
```

表示启动时加载：

```
application.yml
application-dev.yml
```

也就是：

```
公共配置 + 开发环境配置
```

以后你可以扩展：

```
application-test.yml
application-prod.yml
```

------

#### 2.4.3. 服务端口

```
server:
  port: 8080
```

启动后访问接口：

```
http://localhost:8080/api/v1/products
```

注意：这里**不要配置**：

```
server:
  servlet:
    context-path: /api/v1
```

因为你的 Controller 已经会写：

```
@RequestMapping("/api/v1/products")
```

如果再配置 `context-path: /api/v1`，实际路径会变成：

```
/api/v1/api/v1/products
```

这会导致接口路径混乱。

------

#### 2.4.4. 编码配置

```
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

作用：统一请求和响应编码，避免中文乱码。

------

#### 2.4.5. 数据源配置

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_inventory?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

字段说明：

| 配置                | 说明           |
| ------------------- | -------------- |
| `url`               | MySQL 连接地址 |
| `username`          | 数据库用户名   |
| `password`          | 数据库密码     |
| `driver-class-name` | MySQL 驱动类   |

你的库名是：

```
order_inventory
```

所以 URL 中写：

```
/order_inventory
```

如果你执行 DDL 时建的是其他库名，这里必须同步修改。

------

#### 2.5.6. Hikari 连接池配置

Spring Boot 默认使用 HikariCP 作为连接池。第一版配置如下即可：

```
hikari:
  pool-name: OrderInventoryHikariPool
  minimum-idle: 5
  maximum-pool-size: 10
  idle-timeout: 600000
  max-lifetime: 1800000
  connection-timeout: 30000
```

字段说明：

| 配置                 | 说明                           |
| -------------------- | ------------------------------ |
| `pool-name`          | 连接池名称                     |
| `minimum-idle`       | 最小空闲连接数                 |
| `maximum-pool-size`  | 最大连接数                     |
| `idle-timeout`       | 空闲连接最大存活时间，单位毫秒 |
| `max-lifetime`       | 连接最大生命周期，单位毫秒     |
| `connection-timeout` | 获取连接超时时间，单位毫秒     |

第一版项目访问量很小：

```
minimum-idle: 5
maximum-pool-size: 10
```

够用。

------

#### 2.4.7. MyBatis-Plus XML 路径

```
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```

对应你的 XML 文件位置：

```
src/main/resources/mapper/product/ProductMapper.xml
src/main/resources/mapper/stock/ProductStockMapper.xml
src/main/resources/mapper/stock/StockFlowMapper.xml
src/main/resources/mapper/order/OrderInfoMapper.xml
src/main/resources/mapper/order/OrderItemMapper.xml
```

这个配置的作用是让 MyBatis-Plus 能找到你写的 XML SQL。

------

#### 2.4.8. 实体类别名包

```
mybatis-plus:
  type-aliases-package: com.example.orderinventory.*.entity
```

对应你的实体类目录：

```
com.example.orderinventory.product.entity
com.example.orderinventory.stock.entity
com.example.orderinventory.order.entity
```

第一版可以配置，方便 XML 里使用实体类型。

------

#### 2.4.9. 下划线转驼峰

```
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

作用：

数据库字段：

```
product_name
sale_price
create_time
update_time
is_deleted
```

Java 字段：

```
productName
salePrice
createTime
updateTime
isDeleted
```

开启后，MyBatis 会做下划线到驼峰的映射。

------

#### 2.4.10. SQL 日志输出

```
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

开发阶段建议开启。

你调用接口时，控制台能看到 SQL，例如：

```
SELECT id, sku_code, product_name, sale_price
FROM product
WHERE is_deleted = 0
```

这对你学习 MyBatis / MyBatis-Plus 很重要。

后续生产环境一般不这样输出 SQL，而是接入日志框架或 SQL 监控工具。第一版学习项目可以开启。

------

#### 2.4.11. MyBatis-Plus Banner

```
mybatis-plus:
  global-config:
    banner: false
```

作用：关闭 MyBatis-Plus 启动 Banner，让日志更干净。

------

#### 2.4.12. 逻辑删除配置

```
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

这里要特别注意：

```
logic-delete-field: isDeleted
```

写的是 **Java 实体类属性名**，不是数据库字段名。

正确：

```
isDeleted
```

不是：

```
is_deleted
```

因为你的实体类字段应该是：

```
private Integer isDeleted;
```

数据库字段才是：

```
is_deleted
```

官方配置说明里也明确：`logic-delete-field` 配置的是实体类属性名。

------

#### 2.4.13. Jackson 时间格式

```
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
```

作用：

返回 JSON 时，时间统一格式：

```
"createTime": "2026-06-29 15:30:20"
```

`default-property-inclusion: non_null` 表示：

```
值为 null 的字段不返回
```

例如 `remark` 为空时可以不返回。

如果你希望所有字段都返回，包括 `null`，可以删掉这一行：

```
default-property-inclusion: non_null
```

------

#### 2.4.14. 日志级别

```
logging:
  level:
    root: info
    com.example.orderinventory: debug
    com.example.orderinventory.product.mapper: debug
    com.example.orderinventory.stock.mapper: debug
    com.example.orderinventory.order.mapper: debug
```

作用：

| 配置                                | 说明                   |
| ----------------------------------- | ---------------------- |
| `root: info`                        | 全局日志级别           |
| `com.example.orderinventory: debug` | 你自己项目包开启 debug |
| `*.mapper: debug`                   | Mapper 层日志更详细    |

第一版调试接口时建议这样配。

------

### 2.5、最终推荐版

你可以直接复制下面两个文件。

#### 2.5.1 `application.yml`

```
spring:
  application:
    name: order-inventory

  profiles:
    active: dev

server:
  port: 8080

  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml

  type-aliases-package: com.example.orderinventory.*.entity

  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    banner: false
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    root: info
    com.example.orderinventory: debug
    com.example.orderinventory.product.mapper: debug
    com.example.orderinventory.stock.mapper: debug
    com.example.orderinventory.order.mapper: debug
```

#### 2.5.2`application-dev.yml`

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_inventory?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      pool-name: OrderInventoryHikariPool
      minimum-idle: 5
      maximum-pool-size: 10
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
```

------

### 2.6、启动前检查清单

启动项目之前检查这 8 项：

```
1. MySQL 是否已启动。
2. 数据库 order_inventory 是否已创建。
3. product、product_stock、order_info、order_item、stock_flow 是否已建表。
4. application-dev.yml 中 username 是否正确。
5. application-dev.yml 中 password 是否正确。
6. application-dev.yml 中端口 3306 是否和本机 MySQL 一致。
7. mapper XML 是否放在 src/main/resources/mapper 下面。
8. 启动类是否在 com.example.orderinventory 包下。
```

启动成功后，控制台不应该出现：

```
Failed to configure a DataSource
```

也不应该出现：

```
Invalid bound statement
```

如果出现 `Invalid bound statement`，优先检查：

```
1. mapper-locations 是否正确。
2. XML namespace 是否等于 Mapper 接口全限定名。
3. XML 里的 id 是否等于 Mapper 方法名。
4. XML 文件是否在 resources/mapper 目录下。
```

------

### 2.7、关于雪花 ID 的配置建议

第一版我建议你不要只依赖全局配置，而是在 `BaseEntity` 的 `id` 字段上明确写：

```
@TableId(type = IdType.ASSIGN_ID)
private Long id;
```

原因：你每张表都继承 `BaseEntity`，这样最直观，也最不容易遗漏。

也就是说：

```
application.yml 负责通用运行配置
BaseEntity 负责主键生成策略
```

不要把所有规则都堆进配置文件。

## 2. 接口总体规范

### 2.1 基础路径

```text
/api/v1
```

### 2.2 请求数据格式

```http
Content-Type: application/json
Accept: application/json
```

### 2.3 请求方法约定

| 场景 | HTTP 方法 | 说明 |
|---|---|---|
| 新增资源 | `POST` | 例如新增商品、创建订单 |
| 查询资源 | `GET` | 例如分页查询、详情查询 |
| 修改状态 | `PATCH` | 例如商品上下架 |
| 业务动作 | `POST` | 例如取消订单、初始化库存 |

### 2.4 时间格式

统一使用：

```text
yyyy-MM-dd HH:mm:ss
```

示例：

```json
{
  "createTime": "2026-06-29 15:30:20"
}
```

### 2.5 金额单位

所有金额字段统一使用“分”，类型为 `Long`。

示例：

```json
{
  "salePrice": 1999
}
```

表示：

```text
19.99 元
```

第一版禁止在接口中使用小数金额，例如 `19.99`。

### 2.6 主键 ID 规则

所有 `id` 字段由后端通过 MyBatis-Plus 雪花算法生成。

客户端在新增类接口中不传 `id`。

---

## 3. 统一返回结构

### 3.1 返回格式

所有接口统一返回：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 3.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `code` | String | 是 | 业务状态码 |
| `message` | String | 是 | 返回消息 |
| `data` | Object | 否 | 返回数据 |
| `success` | Boolean | 是 | 是否成功 |
| `timestamp` | String | 是 | 响应时间 |

### 3.3 成功响应示例

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 1987654321000000001
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 3.4 失败响应示例

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "商品不存在",
  "data": null,
  "success": false,
  "timestamp": "2026-06-29 15:30:20"
}
```

---

## 4. 分页返回结构

### 4.1 返回格式

```json
{
  "records": [],
  "pageNo": 1,
  "pageSize": 10,
  "total": 100,
  "pages": 10
}
```

### 4.2 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `records` | Array | 当前页数据 |
| `pageNo` | Integer | 当前页码 |
| `pageSize` | Integer | 每页条数 |
| `total` | Long | 总记录数 |
| `pages` | Long | 总页数 |

### 4.3 分页参数规则

| 参数 | 类型 | 必填 | 规则 |
|---|---|---|---|
| `pageNo` | Integer | 否 | 默认 1，最小 1 |
| `pageSize` | Integer | 否 | 默认 10，最小 1，最大 100 |

---

## 5. 通用错误码

| 错误码 | HTTP 状态码 | 说明 |
|---|---:|---|
| `SUCCESS` | 200 | 操作成功 |
| `PARAM_ERROR` | 400 | 参数校验失败 |
| `PRODUCT_NOT_FOUND` | 404 | 商品不存在 |
| `PRODUCT_STATUS_INVALID` | 409 | 商品状态非法 |
| `PRODUCT_SKU_DUPLICATE` | 409 | 商品 SKU 编码重复 |
| `STOCK_NOT_FOUND` | 404 | 库存记录不存在 |
| `STOCK_ALREADY_EXISTS` | 409 | 库存记录已存在 |
| `STOCK_NOT_ENOUGH` | 409 | 库存不足 |
| `ORDER_NOT_FOUND` | 404 | 订单不存在 |
| `ORDER_STATUS_INVALID` | 409 | 订单状态非法 |
| `ORDER_ITEM_EMPTY` | 400 | 订单明细不能为空 |
| `ORDER_ITEM_DUPLICATE` | 400 | 订单中存在重复商品 |
| `CONCURRENT_UPDATE_FAILED` | 409 | 并发更新失败 |
| `SYSTEM_ERROR` | 500 | 系统异常 |

---

## 6. 枚举定义

### 6.1 商品状态 `productStatus`

| 值 | 含义 |
|---:|---|
| `0` | 下架 |
| `1` | 上架 |

### 6.2 订单状态 `orderStatus`

| 值 | 含义 |
|---:|---|
| `10` | 已创建 |
| `20` | 已取消 |
| `30` | 已完成，第一版预留 |

第一版只实现：

```text
已创建 → 已取消
```

### 6.3 库存流水业务类型 `bizType`

| 值 | 含义 |
|---:|---|
| `1` | 下单扣减库存 |
| `2` | 取消订单回滚库存 |
| `3` | 人工初始化库存 |

### 6.4 操作来源 `operatorType`

| 值 | 含义 |
|---:|---|
| `0` | 系统 |
| `1` | 用户 |
| `2` | 管理员 |

---

## 6.异常类

### 6.1`GlobalExceptionHandler` 负责统一异常

至少处理：

```
BusinessException
MethodArgumentNotValidException
ConstraintViolationException
Exception
```

这一步写完后，先造一个测试接口，确认异常返回结构符合文档。

### 6.2全局异常类BusinessException



## 6.公共实体类、实体类、Mapper、XML

### 6.1`BaseEntity` 负责审计字段

统一放：

```
id
createTime
updateTime
isDeleted
version
```

实体类继承 `BaseEntity`。

## 7. 第一版接口清单

| 序号 | 接口 | 方法 | 路径 |
|---:|---|---|---|
| 1 | 新增商品 | `POST` | `/api/v1/products` |
| 2 | 商品分页查询 | `GET` | `/api/v1/products` |
| 3 | 修改商品上下架状态 | `PATCH` | `/api/v1/products/{productId}/status` |
| 4 | 初始化库存 | `POST` | `/api/v1/stocks/init` |
| 5 | 创建订单 | `POST` | `/api/v1/orders` |
| 6 | 取消订单 | `POST` | `/api/v1/orders/{orderNo}/cancel` |
| 7 | 查询订单详情 | `GET` | `/api/v1/orders/{orderNo}` |
| 8 | 查询库存流水 | `GET` | `/api/v1/stock-flows` |

---

## 8. 商品模块接口

---

### 8.1 新增商品

#### 8.1.1 基本信息

```http
POST /api/v1/products
```

#### 8.1.2 接口说明

新增商品基础信息。

新增成功后，系统只创建 `product` 记录，不自动创建库存记录。库存需要调用“初始化库存”接口单独处理。

#### 8.1.3 请求参数

```json
{
  "skuCode": "SKU-APPLE-001",
  "productName": "苹果手机",
  "productStatus": 1,
  "salePrice": 599900,
  "remark": "测试商品"
}
```

#### 8.1.4 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `skuCode` | String | 是 | 长度 1-64 | 商品 SKU 编码，唯一 |
| `productName` | String | 是 | 长度 1-128 | 商品名称 |
| `productStatus` | Integer | 是 | 只能是 0 或 1 | 商品状态 |
| `salePrice` | Long | 是 | 大于 0 | 销售单价，单位：分 |
| `remark` | String | 否 | 最大 512 | 备注 |

#### 8.1.5 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 1987654321000000001,
    "skuCode": "SKU-APPLE-001",
    "productName": "苹果手机",
    "productStatus": 1,
    "salePrice": 599900,
    "remark": "测试商品",
    "createTime": "2026-06-29 15:30:20"
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

#### 8.1.6 业务规则

```text
1. skuCode 不能为空。
2. skuCode 在 product 表中必须唯一。
3. productName 不能为空。
4. salePrice 必须大于 0。
5. productStatus 只能为 0 或 1。
6. 新增商品时，不允许客户端传 id。
```

#### 8.1.7 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 参数格式错误 |
| `PRODUCT_SKU_DUPLICATE` | SKU 编码已存在 |

---

## 8.2 商品分页查询

### 8.2.1 基本信息

```http
GET /api/v1/products
```

### 8.2.2 接口说明

 

### 8.2.3 请求示例

```http
GET /api/v1/products?pageNo=1&pageSize=10&keyword=苹果&productStatus=1
```

### 8.2.4 Query 参数说明

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNo` | Integer | 否 | 页码，默认 1 |
| `pageSize` | Integer | 否 | 每页条数，默认 10，最大 100 |
| `keyword` | String | 否 | 商品名称或 SKU 编码模糊查询 |
| `productStatus` | Integer | 否 | 商品状态：0-下架，1-上架 |

### 8.2.5 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",f
  "data": {
    "records": [
      {
        "id": 1987654321000000001,
        "skuCode": "SKU-APPLE-001",
        "productName": "苹果手机",
        "productStatus": 1,
        "salePrice": 599900,
        "remark": "测试商品",
        "createTime": "2026-06-29 15:30:20",
        "updateTime": "2026-06-29 15:30:20"
      }
    ],
    "pageNo": 1,
    "pageSize": 10,
    "total": 1,
    "pages": 1
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 8.2.6 业务规则

```text
1. 默认只查询 is_deleted = 0 的数据。
2. keyword 非空时，匹配 sku_code 或 product_name。
3. productStatus 非空时，按商品状态筛选。
4. 按 create_time 倒序排序。
```

### 8.2.7 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 分页参数非法 |

---

## 8.3 修改商品上下架状态

### 8.3.1 基本信息

```http
PATCH /api/v1/products/{productId}/status
```

### 8.3.2 接口说明

修改商品状态。

### 8.3.3 Path 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `productId` | Long | 是 | 商品 ID |

### 8.3.4 请求参数

```json
{
  "productStatus": 0
}
```

### 8.3.5 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `productStatus` | Integer | 是 | 只能是 0 或 1 | 商品状态 |

### 8.3.6 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": 1987654321000000001,
    "productStatus": 0
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 8.3.7 业务规则

```text
1. 商品必须存在。
2. 已逻辑删除的商品不能修改状态。
3. productStatus 只能是 0 或 1。
4. 下架商品不能被下单。
```

### 8.3.8 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 参数非法 |
| `PRODUCT_NOT_FOUND` | 商品不存在 |
| `CONCURRENT_UPDATE_FAILED` | 并发更新失败 |

---

# 9. 库存模块接口

---

## 9.1 初始化库存

### 9.1.1 基本信息

```http
POST /api/v1/stocks/init
```

### 9.1.2 接口说明

给商品初始化库存。

第一版规则：一个商品只能初始化一次库存。后续要调整库存，可以在第二版增加“库存调整接口”。

### 9.1.3 请求参数

```json
{
  "productId": 1987654321000000001,
  "availableQuantity": 100,
  "operatorId": 10001,
  "remark": "初始化库存"
}
```

### 9.1.4 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `productId` | Long | 是 | 大于 0 | 商品 ID |
| `availableQuantity` | Integer | 是 | 大于等于 0 | 初始可用库存 |
| `operatorId` | Long | 否 | 大于 0 | 操作人 ID |
| `remark` | String | 否 | 最大 512 | 备注 |

### 9.1.5 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "productId": 1987654321000000001,
    "availableQuantity": 100,
    "lockedQuantity": 0,
    "totalInQuantity": 100,
    "totalOutQuantity": 0
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 9.1.6 业务规则

```text
1. productId 对应的商品必须存在。
2. 已逻辑删除的商品不能初始化库存。
3. 同一个商品只能存在一条 product_stock 记录。
4. 如果 product_stock 已存在，返回 STOCK_ALREADY_EXISTS。
5. 初始化库存时，需要同时插入 stock_flow。
6. stock_flow.bizType = 3。
7. stock_flow.changeQuantity = availableQuantity。
8. stock_flow.beforeQuantity = 0。
9. stock_flow.afterQuantity = availableQuantity。
10.第一版stock_flow.bizNo = "STOCK_INIT_" + productId
11.第一版stock_flow.operatorType=0
12.stock_flow.remark=remark
```

### 9.1.7 事务要求

该接口需要事务：

```java
@Transactional(rollbackFor = Exception.class)
```

原因：

```text
1. 需要新增 product_stock。
2. 需要新增 stock_flow。
3. 两步必须同时成功或同时失败。
```

### 9.1.8 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 参数非法 |
| `PRODUCT_NOT_FOUND` | 商品不存在 |
| `STOCK_ALREADY_EXISTS` | 库存记录已存在 |
| `SYSTEM_ERROR` | 系统异常 |

---

# 10. 订单模块接口

---

## 10.1 创建订单

### 10.1.1 基本信息

```http
POST /api/v1/orders
```

### 10.1.2 接口说明

创建订单，并扣减库存。

这是第一版项目最核心的接口，必须体现：

```text
参数校验
业务校验
库存扣减
订单保存
订单明细保存
库存流水保存
事务回滚
```

### 10.1.3 请求参数

```json
{
  "buyerId": 10001,
  "items": [
    {
      "productId": 1987654321000000001,
      "quantity": 2
    },
    {
      "productId": 1987654321000000002,
      "quantity": 1
    }
  ],
  "remark": "测试下单"
}
```

### 10.1.4 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `buyerId` | Long | 是 | 大于 0 | 下单用户 ID |
| `items` | Array | 是 | 至少 1 条 | 订单商品明细 |
| `items[].productId` | Long | 是 | 大于 0 | 商品 ID |
| `items[].quantity` | Integer | 是 | 大于 0 | 购买数量 |
| `remark` | String | 否 | 最大 512 | 备注 |

### 10.1.5 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "orderNo": "ORD202606291530201987654321",
    "buyerId": 10001,
    "orderStatus": 10,
    "orderStatusName": "已创建",
    "totalAmount": 1199800,
    "totalQuantity": 2,
    "createTime": "2026-06-29 15:30:20"
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 10.1.6 业务规则

```text
1. buyerId 不能为空。
2. items 不能为空。
3. items 中 productId 不能为空。
4. items 中 quantity 必须大于 0。
5. 同一订单内不允许出现重复 productId。
6. 商品必须存在。
7. 商品必须是上架状态。
8. 商品必须存在库存记录。
9. 商品库存必须充足。
10. 创建订单时，需要扣减 product_stock.available_quantity。
11. 创建订单时，需要增加 product_stock.total_out_quantity。
12. 创建订单时，需要保存 order_info。
13. 创建订单时，需要保存 order_item。
14. 创建订单时，需要保存 stock_flow。
15. 任意一步失败，整个订单创建流程必须回滚。
```

### 10.1.7 库存扣减规则

假设用户购买数量为 `quantity`。

扣减前：

```text
beforeQuantity = product_stock.available_quantity
```

扣减后：

```text
afterQuantity = beforeQuantity - quantity
```

需要满足：

```text
beforeQuantity >= quantity
```

建议 SQL 更新条件包含：

```sql
available_quantity >= #{quantity}
AND is_deleted = 0
```

更新结果必须判断影响行数：

```text
影响行数 = 1：扣减成功
影响行数 = 0：库存不足或并发更新失败
```

### 10.1.8 库存流水规则

下单成功后，每个商品插入一条库存流水。

| 字段 | 取值 |
|---|---|
| `bizNo` | 订单编号 |
| `bizType` | `1` |
| `changeQuantity` | `-quantity` |
| `beforeQuantity` | 扣减前库存 |
| `afterQuantity` | 扣减后库存 |
| `operatorId` | buyerId |
| `operatorType` | `1` |

### 10.1.9 订单金额计算规则

```text
order_item.item_amount = product.sale_price * quantity
order_info.total_amount = 所有 item_amount 之和
order_info.total_quantity = 所有 quantity 之和
```

金额以分为单位，禁止使用小数。

### 10.1.10 事务要求

该接口必须加事务：

```java
@Transactional(rollbackFor = Exception.class)
```

涉及表：

```text
product_stock
order_info
order_item
stock_flow
```

### 10.1.11 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 参数非法 |
| `ORDER_ITEM_EMPTY` | 订单明细为空 |
| `ORDER_ITEM_DUPLICATE` | 订单内存在重复商品 |
| `PRODUCT_NOT_FOUND` | 商品不存在 |
| `PRODUCT_STATUS_INVALID` | 商品不是上架状态 |
| `STOCK_NOT_FOUND` | 库存记录不存在 |
| `STOCK_NOT_ENOUGH` | 库存不足 |
| `CONCURRENT_UPDATE_FAILED` | 并发更新失败 |

---

## 10.2 取消订单

### 10.2.1 基本信息

```http
POST /api/v1/orders/{orderNo}/cancel
```

### 10.2.2 接口说明

取消已创建订单，并回滚库存。

### 10.2.3 Path 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `orderNo` | String | 是 | 订单编号 |

### 10.2.4 请求参数

```json
{
  "cancelReason": "用户主动取消"
}
```

### 10.2.5 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `cancelReason` | String | 是 | 长度 1-255 | 取消原因 |

### 10.2.6 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "orderNo": "ORD202606291530201987654321",
    "orderStatus": 20,
    "orderStatusName": "已取消",
    "cancelReason": "用户主动取消",
    "updateTime": "2026-06-29 15:40:20"
  },
  "success": true,
  "timestamp": "2026-06-29 15:40:20"
}
```

### 10.2.7 业务规则

```text
1. orderNo 不能为空。
2. 订单必须存在。
3. 只有 orderStatus = 10 的订单允许取消。
4. orderStatus = 20 的订单不能重复取消。
5. 取消订单时，需要回滚订单明细中的库存。
6. 取消订单时，需要插入库存流水。
7. 取消订单时，需要更新 order_info.order_status = 20。
8. 任意一步失败，整体回滚。
```

### 10.2.8 库存回滚规则

对订单中的每一条 `order_item`：

```text
回滚数量 = order_item.quantity
```

回滚前：

```text
beforeQuantity = product_stock.available_quantity
```

回滚后：

```text
afterQuantity = beforeQuantity + order_item.quantity
```

更新规则：

```text
product_stock.available_quantity 增加 quantity
product_stock.total_out_quantity 减少 quantity
```

### 10.2.9 库存流水规则

取消订单时，每个商品插入一条库存流水。

| 字段 | 取值 |
|---|---|
| `bizNo` | 订单编号 |
| `bizType` | `2` |
| `changeQuantity` | `quantity` |
| `beforeQuantity` | 回滚前库存 |
| `afterQuantity` | 回滚后库存 |
| `operatorType` | `1` |

### 10.2.10 事务要求

该接口必须加事务：

```java
@Transactional(rollbackFor = Exception.class)
```

涉及表：

```text
order_info
order_item
product_stock
stock_flow
```

### 10.2.11 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 参数非法 |
| `ORDER_NOT_FOUND` | 订单不存在 |
| `ORDER_STATUS_INVALID` | 当前订单状态不允许取消 |
| `STOCK_NOT_FOUND` | 库存记录不存在 |
| `CONCURRENT_UPDATE_FAILED` | 并发更新失败 |

---

## 10.3 查询订单详情

### 10.3.1 基本信息

```http
GET /api/v1/orders/{orderNo}
```

### 10.3.2 接口说明

根据订单编号查询订单详情，包含订单主信息和订单明细列表。

### 10.3.3 Path 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `orderNo` | String | 是 | 订单编号 |

### 10.3.4 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "orderNo": "ORD202606291530201987654321",
    "buyerId": 10001,
    "orderStatus": 10,
    "orderStatusName": "已创建",
    "totalAmount": 1199800,
    "totalQuantity": 2,
    "remark": "测试下单",
    "createTime": "2026-06-29 15:30:20",
    "updateTime": "2026-06-29 15:30:20",
    "items": [
      {
        "productId": 1987654321000000001,
        "skuCode": "SKU-APPLE-001",
        "productName": "苹果手机",
        "salePrice": 599900,
        "quantity": 2,
        "itemAmount": 1199800
      }
    ]
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 10.3.5 业务规则

```text
1. orderNo 不能为空。
2. 订单必须存在。
3. 查询 order_info。
4. 查询 order_item。
5. 返回订单主信息和订单明细列表。
6. 默认只查询 is_deleted = 0 的数据。
```

### 10.3.6 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 参数非法 |
| `ORDER_NOT_FOUND` | 订单不存在 |

---

# 11. 库存流水接口

---

## 11.1 查询库存流水

### 11.1.1 基本信息

```http
GET /api/v1/stock-flows
```

### 11.1.2 接口说明

分页查询库存流水。

该接口主要用于排查库存变化原因。

### 11.1.3 请求示例

```http
GET /api/v1/stock-flows?pageNo=1&pageSize=10&productId=1987654321000000001&bizType=1
```

### 11.1.4 Query 参数说明

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNo` | Integer | 否 | 页码，默认 1 |
| `pageSize` | Integer | 否 | 每页条数，默认 10，最大 100 |
| `productId` | Long | 否 | 商品 ID |
| `skuCode` | String | 否 | 商品 SKU 编码 |
| `bizNo` | String | 否 | 业务单号，例如订单编号 |
| `bizType` | Integer | 否 | 业务类型：1-下单扣减，2-取消回滚，3-初始化库存 |
| `startTime` | String | 否 | 开始时间，格式：yyyy-MM-dd HH:mm:ss |
| `endTime` | String | 否 | 结束时间，格式：yyyy-MM-dd HH:mm:ss |

### 11.1.5 成功响应

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1987654321000000101,
        "productId": 1987654321000000001,
        "skuCode": "SKU-APPLE-001",
        "bizNo": "ORD202606291530201987654321",
        "bizType": 1,
        "bizTypeName": "下单扣减库存",
        "changeQuantity": -2,
        "beforeQuantity": 100,
        "afterQuantity": 98,
        "operatorId": 10001,
        "operatorType": 1,
        "operatorTypeName": "用户",
        "remark": "创建订单扣减库存",
        "createTime": "2026-06-29 15:30:20"
      }
    ],
    "pageNo": 1,
    "pageSize": 10,
    "total": 1,
    "pages": 1
  },
  "success": true,
  "timestamp": "2026-06-29 15:30:20"
}
```

### 11.1.6 业务规则

```text
1. 默认只查询 is_deleted = 0 的数据。
2. productId 非空时，按商品 ID 查询。
3. skuCode 非空时，按 SKU 编码查询。
4. bizNo 非空时，按业务单号查询。
5. bizType 非空时，按业务类型查询。
6. startTime 和 endTime 非空时，按 create_time 范围查询。
7. 默认按 create_time 倒序排序。
```

### 11.1.7 可能错误

| 错误码 | 说明 |
|---|---|
| `PARAM_ERROR` | 参数非法 |

---

# 12. Controller 层方法建议

## 12.1 ProductController

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @PostMapping
    public ApiResult<ProductVO> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ApiResult.success(productService.createProduct(request));
    }

    @GetMapping
    public ApiResult<PageResult<ProductVO>> pageProducts(@Valid ProductPageQuery query) {
        return ApiResult.success(productService.pageProducts(query));
    }

    @PatchMapping("/{productId}/status")
    public ApiResult<ProductStatusVO> updateProductStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request) {
        return ApiResult.success(productService.updateProductStatus(productId, request));
    }
}
```

## 12.2 OrderController

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @PostMapping
    public ApiResult<OrderCreateVO> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResult.success(orderService.createOrder(request));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResult<OrderCancelVO> cancelOrder(
            @PathVariable String orderNo,
            @Valid @RequestBody OrderCancelRequest request) {
        return ApiResult.success(orderService.cancelOrder(orderNo, request));
    }

    @GetMapping("/{orderNo}")
    public ApiResult<OrderDetailVO> getOrderDetail(@PathVariable String orderNo) {
        return ApiResult.success(orderService.getOrderDetail(orderNo));
    }
}
```

## 12.3 StockController

```java
@RestController
@RequestMapping("/api/v1")
public class StockController {

    @PostMapping("/stocks/init")
    public ApiResult<ProductStockVO> initStock(@Valid @RequestBody StockInitRequest request) {
        return ApiResult.success(stockService.initStock(request));
    }

    @GetMapping("/stock-flows")
    public ApiResult<PageResult<StockFlowVO>> pageStockFlows(@Valid StockFlowPageQuery query) {
        return ApiResult.success(stockService.pageStockFlows(query));
    }
}
```

---

# 13. 第一版开发顺序

建议严格按下面顺序实现：

```text
1. 统一返回 ApiResult
2. 错误码 ErrorCode
3. 业务异常 BusinessException
4. 全局异常处理 GlobalExceptionHandler
5. BaseEntity
6. MyBatis-Plus 配置
7. Product 新增
8. Product 分页查询
9. Product 上下架
10. Stock 初始化
11. Order 创建
12. Order 取消
13. Order 详情
14. StockFlow 分页查询
```

不要一开始就写订单接口。订单接口依赖商品和库存，先把商品、库存跑通。

---

# 14. 第一版验收标准

## 14.1 商品模块

```text
1. 新增商品成功。
2. 重复 skuCode 新增失败。
3. 商品分页查询成功。
4. 商品上架成功。
5. 商品下架后不能下单。
```

## 14.2 库存模块

```text
1. 商品初始化库存成功。
2. 同一个商品重复初始化库存失败。
3. 查询库存流水成功。
```

## 14.3 订单模块

```text
1. 上架商品且库存充足，下单成功。
2. 下单成功后，库存减少。
3. 下单成功后，生成订单主表。
4. 下单成功后，生成订单明细。
5. 下单成功后，生成库存流水。
6. 库存不足，下单失败。
7. 下单失败时，订单主表、订单明细、库存流水不能产生脏数据。
8. 取消订单成功后，库存回滚。
9. 取消订单成功后，生成库存回滚流水。
10. 已取消订单不能重复取消。
```

---

# 15. 第一版核心接口链路

## 15.1 创建订单链路

```text
POST /api/v1/orders
→ Controller 接收 OrderCreateRequest
→ 参数校验 buyerId、items、productId、quantity
→ Service 校验商品存在
→ Service 校验商品上架
→ Service 校验库存存在
→ Service 校验库存充足
→ 扣减 product_stock.available_quantity
→ 保存 order_info
→ 保存 order_item
→ 保存 stock_flow
→ 事务提交
→ 返回 OrderCreateVO
```

## 15.2 创建订单失败回滚链路

```text
POST /api/v1/orders
→ 扣减库存成功
→ 保存订单失败
→ 抛出异常
→ @Transactional 感知异常
→ 回滚 product_stock、order_info、order_item、stock_flow
→ GlobalExceptionHandler 统一处理异常
→ ApiResult 返回失败结果
```

## 15.3 取消订单链路

```text
POST /api/v1/orders/{orderNo}/cancel
→ Controller 接收 orderNo 和 OrderCancelRequest
→ 参数校验 cancelReason
→ Service 查询订单
→ 校验订单状态为已创建
→ 查询订单明细
→ 回滚每个商品库存
→ 保存库存回滚流水
→ 更新订单状态为已取消
→ 事务提交
→ 返回 OrderCancelVO
```

---

# 16. 后续第二版扩展方向

第一版完成后，可以考虑以下扩展：

```text
1. 增加用户表和登录认证。
2. 增加库存调整接口。
3. 增加订单分页查询接口。
4. 增加 Redis 商品详情缓存。
5. 增加接口防重复提交。
6. 增加操作日志 AOP。
7. 增加订单超时自动取消。
8. 增加 MQ 异步库存流水或通知。
```

第一版不要提前加入这些能力，避免项目范围失控。
