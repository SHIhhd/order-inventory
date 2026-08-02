package com.example.orderinventory.order.mapper;

import com.example.orderinventory.order.mapper.model.OrderDetailQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-detail-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
@ActiveProfiles("test")
@Transactional
class OrderInfoMapperDetailTest {

    private static final String ORDER_NO = "ORD202606291530201987654321";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @BeforeEach
    void setUpSchemaAndData() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS order_info (
                    id BIGINT PRIMARY KEY,
                    order_no VARCHAR(64) NOT NULL,
                    buyer_id BIGINT NOT NULL,
                    order_status TINYINT NOT NULL,
                    total_amount BIGINT NOT NULL,
                    total_quantity INT NOT NULL,
                    remark VARCHAR(512),
                    create_time TIMESTAMP NOT NULL,
                    update_time TIMESTAMP NOT NULL,
                    is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS order_item (
                    id BIGINT PRIMARY KEY,
                    order_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    sku_code VARCHAR(64) NOT NULL,
                    product_name VARCHAR(128) NOT NULL,
                    sale_price BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    item_amount BIGINT NOT NULL,
                    is_deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.update("DELETE FROM order_item");
        jdbcTemplate.update("DELETE FROM order_info");
    }

    @Test
    void selectOrderDetailShouldMapItemsInIdOrderAndIgnoreDeletedItems() {
        insertOrder(1L, ORDER_NO, 0);
        insertItem(30L, 1L, 3003L, "SKU-003", 0);
        insertItem(10L, 1L, 1001L, "SKU-001", 0);
        insertItem(20L, 1L, 2002L, "SKU-DELETED", 1);

        OrderDetailQueryResult result = orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNo()).isEqualTo(ORDER_NO);
        assertThat(result.getBuyerId()).isEqualTo(10001L);
        assertThat(result.getOrderStatus()).isEqualTo(10);
        assertThat(result.getItems())
                .extracting(item -> item.getProductId())
                .containsExactly(1001L, 3003L);
        assertThat(result.getItems())
                .extracting(item -> item.getSkuCode())
                .containsExactly("SKU-001", "SKU-003");
    }

    @Test
    void selectOrderDetailShouldReturnNullForDeletedOrder() {
        insertOrder(1L, ORDER_NO, 1);
        insertItem(10L, 1L, 1001L, "SKU-001", 0);

        OrderDetailQueryResult result = orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO);

        assertThat(result).isNull();
    }

    @Test
    void selectOrderDetailShouldReturnEmptyItemsWhenNoActiveItemExists() {
        insertOrder(1L, ORDER_NO, 0);
        insertItem(10L, 1L, 1001L, "SKU-DELETED", 1);

        OrderDetailQueryResult result = orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void selectOrderDetailShouldNotAttachItemsFromAnotherOrder() {
        insertOrder(1L, ORDER_NO, 0);
        insertOrder(2L, "ORD-OTHER", 0);
        insertItem(10L, 1L, 1001L, "SKU-OWN", 0);
        insertItem(20L, 2L, 2002L, "SKU-OTHER", 0);

        OrderDetailQueryResult result = orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO);

        assertThat(result.getItems())
                .extracting(item -> item.getSkuCode())
                .containsExactly("SKU-OWN");
    }

    private void insertOrder(long id, String orderNo, int deleted) {
        LocalDateTime time = LocalDateTime.of(2026, 6, 29, 15, 30, 20);
        jdbcTemplate.update("""
                        INSERT INTO order_info (
                            id, order_no, buyer_id, order_status, total_amount,
                            total_quantity, remark, create_time, update_time, is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, orderNo, 10001L, 10, 1_199_800L,
                2, "测试下单", time, time, deleted);
    }

    private void insertItem(long id, long orderId, long productId, String skuCode, int deleted) {
        jdbcTemplate.update("""
                        INSERT INTO order_item (
                            id, order_id, product_id, sku_code, product_name,
                            sale_price, quantity, item_amount, is_deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, orderId, productId, skuCode, "测试商品",
                599_900L, 2, 1_199_800L, deleted);
    }
}
