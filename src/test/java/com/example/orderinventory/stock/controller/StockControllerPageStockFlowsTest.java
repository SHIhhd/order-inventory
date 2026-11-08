package com.example.orderinventory.stock.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * README 11.1 {@code GET /api/v1/stock-flows} contract tests.
 *
 * <p>These tests intentionally exercise the complete MVC-to-database path so
 * pagination SQL, logical deletion and response serialization are covered.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stock-flow-page-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StockControllerPageStockFlowsTest {

    private static final String URL = "/api/v1/stock-flows";
    private static final long PRODUCT_ID = 1_987_654_321_000_000_001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchemaAndData() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS stock_flow (
                    id BIGINT PRIMARY KEY,
                    product_id BIGINT NOT NULL,
                    sku_code VARCHAR(64) NOT NULL,
                    biz_no VARCHAR(64) NOT NULL,
                    biz_type TINYINT NOT NULL,
                    change_quantity INT NOT NULL,
                    before_quantity INT NOT NULL,
                    after_quantity INT NOT NULL,
                    operator_id BIGINT,
                    operator_type TINYINT NOT NULL,
                    remark VARCHAR(512),
                    create_time TIMESTAMP NOT NULL,
                    update_time TIMESTAMP NOT NULL,
                    is_deleted TINYINT NOT NULL DEFAULT 0,
                    version INT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.update("DELETE FROM stock_flow");
    }

    @Test
    void pageStockFlowsShouldMatchReadmeResponseAndApplyAllExactFilters() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-APPLE-001", "ORD-001", 1,
                -2, 100, 98, 10001L, 1, "创建订单扣减库存",
                LocalDateTime.of(2026, 6, 29, 15, 30, 20), 0);
        insertFlow(102L, PRODUCT_ID, "SKU-APPLE-001", "ORD-OTHER", 1,
                -1, 98, 97, 10002L, 1, "不应命中",
                LocalDateTime.of(2026, 6, 29, 16, 0), 0);

        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("productId", String.valueOf(PRODUCT_ID))
                        .param("skuCode", "SKU-APPLE-001")
                        .param("bizNo", "ORD-001")
                        .param("bizType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(101))
                .andExpect(jsonPath("$.data.records[0].productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.data.records[0].skuCode").value("SKU-APPLE-001"))
                .andExpect(jsonPath("$.data.records[0].bizNo").value("ORD-001"))
                .andExpect(jsonPath("$.data.records[0].bizType").value(1))
                .andExpect(jsonPath("$.data.records[0].bizTypeName").value("下单扣减库存"))
                .andExpect(jsonPath("$.data.records[0].changeQuantity").value(-2))
                .andExpect(jsonPath("$.data.records[0].beforeQuantity").value(100))
                .andExpect(jsonPath("$.data.records[0].afterQuantity").value(98))
                .andExpect(jsonPath("$.data.records[0].operatorId").value(10001))
                .andExpect(jsonPath("$.data.records[0].operatorType").value(1))
                .andExpect(jsonPath("$.data.records[0].operatorTypeName").value("用户"))
                .andExpect(jsonPath("$.data.records[0].remark").value("创建订单扣减库存"))
                .andExpect(jsonPath("$.data.records[0].createTime").value("2026-06-29 15:30:20"))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pages").value(1));
    }

    @Test
    void pageStockFlowsShouldUseDocumentedPaginationDefaults() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-001", "ORD-001", 1,
                -1, 10, 9, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 15, 30, 20), 0);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void pageStockFlowsShouldReturnDatabaseTotalPagesAndNewestRecordsFirst() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-001", "ORD-OLD", 1,
                -1, 10, 9, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 10, 0), 0);
        insertFlow(102L, PRODUCT_ID, "SKU-001", "ORD-MIDDLE", 1,
                -1, 9, 8, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 11, 0), 0);
        insertFlow(103L, PRODUCT_ID, "SKU-001", "ORD-NEW", 1,
                -1, 8, 7, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 12, 0), 0);
        insertFlow(104L, PRODUCT_ID, "SKU-001", "ORD-DELETED", 1,
                -1, 7, 6, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 13, 0), 1);

        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "2")
                        .param("bizType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].bizNo").value("ORD-NEW"))
                .andExpect(jsonPath("$.data.records[1].bizNo").value("ORD-MIDDLE"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pages").value(2));
    }

    @Test
    void pageStockFlowsShouldUseFullFilteredCountForPaginationMetadata() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-001", "ORD-001", 1,
                -1, 10, 9, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 10, 0), 0);
        insertFlow(102L, PRODUCT_ID, "SKU-001", "ORD-002", 1,
                -1, 9, 8, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 11, 0), 0);
        insertFlow(103L, PRODUCT_ID, "SKU-001", "ORD-003", 1,
                -1, 8, 7, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 12, 0), 0);

        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "2")
                        .param("bizType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pages").value(2));
    }

    @Test
    void pageStockFlowsShouldExcludeLogicallyDeletedRows() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-001", "ORD-ACTIVE", 1,
                -1, 10, 9, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 10, 0), 0);
        insertFlow(102L, PRODUCT_ID, "SKU-001", "ORD-DELETED", 1,
                -1, 9, 8, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 11, 0), 1);

        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("bizType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].bizNo").value("ORD-ACTIVE"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void pageStockFlowsShouldApplyInclusiveCreateTimeRange() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-001", "ORD-BEFORE", 1,
                -1, 10, 9, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 9, 59, 59), 0);
        insertFlow(102L, PRODUCT_ID, "SKU-001", "ORD-START", 1,
                -1, 9, 8, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 10, 0), 0);
        insertFlow(103L, PRODUCT_ID, "SKU-001", "ORD-END", 1,
                -1, 8, 7, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 11, 0), 0);
        insertFlow(104L, PRODUCT_ID, "SKU-001", "ORD-AFTER", 1,
                -1, 7, 6, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 11, 0, 1), 0);

        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("bizType", "1")
                        .param("startTime", "2026-06-29 10:00:00")
                        .param("endTime", "2026-06-29 11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].bizNo").value("ORD-END"))
                .andExpect(jsonPath("$.data.records[1].bizNo").value("ORD-START"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void pageStockFlowsShouldIgnoreBlankOptionalTextFilters() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-001", "ORD-001", 1,
                -1, 10, 9, 10001L, 1, null,
                LocalDateTime.of(2026, 6, 29, 10, 0), 0);

        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("bizType", "1")
                        .param("skuCode", "")
                        .param("bizNo", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void pageStockFlowsShouldMapOperatorNameFromOperatorType() throws Exception {
        insertFlow(101L, PRODUCT_ID, "SKU-001", "STOCK_INIT_1", 3,
                10, 0, 10, null, 0, "初始化库存",
                LocalDateTime.of(2026, 6, 29, 10, 0), 0);

        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("bizType", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].operatorType").value(0))
                .andExpect(jsonPath("$.data.records[0].operatorTypeName").value("系统"));
    }

    @Test
    void pageStockFlowsShouldReturnEmptyPage() throws Exception {
        mockMvc.perform(get(URL)
                        .param("pageNo", "2")
                        .param("pageSize", "10")
                        .param("bizType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.pages").value(0));
    }

    @ParameterizedTest(name = "reject invalid query: {0}")
    @ValueSource(strings = {
            "pageNo=0&pageSize=10&bizType=1",
            "pageNo=1&pageSize=0&bizType=1",
            "pageNo=1&pageSize=101&bizType=1",
            "pageNo=1&pageSize=10&bizType=0",
            "pageNo=1&pageSize=10&bizType=4",
            "pageNo=abc&pageSize=10&bizType=1",
            "pageNo=1&pageSize=abc&bizType=1",
            "pageNo=1&pageSize=10&productId=not-a-long&bizType=1"
    })
    void pageStockFlowsShouldRejectInvalidNumericQuery(String query) throws Exception {
        mockMvc.perform(get(URL + "?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @ParameterizedTest(name = "reject invalid time: {0}")
    @ValueSource(strings = {
            "2026/06/29 10:00:00",
            "2026-06-29",
            "not-a-time"
    })
    void pageStockFlowsShouldRejectInvalidTimeFormat(String startTime) throws Exception {
        mockMvc.perform(get(URL)
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("bizType", "1")
                        .param("startTime", startTime))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));
    }

    private void insertFlow(
            long id,
            long productId,
            String skuCode,
            String bizNo,
            int bizType,
            int changeQuantity,
            int beforeQuantity,
            int afterQuantity,
            Long operatorId,
            int operatorType,
            String remark,
            LocalDateTime createTime,
            int deleted) {
        jdbcTemplate.update("""
                        INSERT INTO stock_flow (
                            id, product_id, sku_code, biz_no, biz_type,
                            change_quantity, before_quantity, after_quantity,
                            operator_id, operator_type, remark, create_time,
                            update_time, is_deleted, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, productId, skuCode, bizNo, bizType,
                changeQuantity, beforeQuantity, afterQuantity,
                operatorId, operatorType, remark, createTime,
                createTime, deleted, 0);
    }
}
