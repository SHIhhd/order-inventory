package com.example.orderinventory.order.controller;

import com.example.orderinventory.common.config.JacksonDateTimeConfig;
import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.exception.GlobalExceptionHandler;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.order.service.OrderService;
import com.example.orderinventory.order.vo.OrderDetailItemResponse;
import com.example.orderinventory.order.vo.OrderDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({GlobalExceptionHandler.class, JacksonDateTimeConfig.class})
class OrderControllerDetailTest {

    private static final String DETAIL_URL = "/api/v1/orders/{orderNo}";
    private static final String ORDER_NO = "ORD202606291530201987654321";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void getOrderDetailShouldMatchReadmeContract() throws Exception {
        LocalDateTime time = LocalDateTime.of(2026, 6, 29, 15, 30, 20);
        OrderDetailResponse response = new OrderDetailResponse(
                ORDER_NO,
                10001L,
                10,
                "已创建",
                1_199_800L,
                2,
                "测试下单",
                time,
                time,
                List.of(new OrderDetailItemResponse(
                        1_987_654_321_000_000_001L,
                        "SKU-APPLE-001",
                        "苹果手机",
                        599_900L,
                        2,
                        1_199_800L
                ))
        );
        when(orderService.getOrderDetail(ORDER_NO)).thenReturn(response);

        mockMvc.perform(get(DETAIL_URL, ORDER_NO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.orderNo").value(ORDER_NO))
                .andExpect(jsonPath("$.data.buyerId").value(10001))
                .andExpect(jsonPath("$.data.orderStatus").value(10))
                .andExpect(jsonPath("$.data.orderStatusName").value("已创建"))
                .andExpect(jsonPath("$.data.totalAmount").value(1_199_800))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.remark").value("测试下单"))
                .andExpect(jsonPath("$.data.createTime").value("2026-06-29 15:30:20"))
                .andExpect(jsonPath("$.data.updateTime").value("2026-06-29 15:30:20"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(1_987_654_321_000_000_001L))
                .andExpect(jsonPath("$.data.items[0].skuCode").value("SKU-APPLE-001"))
                .andExpect(jsonPath("$.data.items[0].productName").value("苹果手机"))
                .andExpect(jsonPath("$.data.items[0].salePrice").value(599_900))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].itemAmount").value(1_199_800))
                .andExpect(jsonPath("$.data.requestId").doesNotExist())
                .andExpect(jsonPath("$.data.cancelReason").doesNotExist());

        verify(orderService).getOrderDetail(ORDER_NO);
    }

    @Test
    void getOrderDetailShouldReturnOrderNotFound() throws Exception {
        when(orderService.getOrderDetail(ORDER_NO))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        mockMvc.perform(get(DETAIL_URL, ORDER_NO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("订单不存在"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getOrderDetailShouldRejectBlankOrderNo() throws Exception {
        mockMvc.perform(get(URI.create("/api/v1/orders/%20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrderDetailShouldRejectOrderNoLongerThan64Characters() throws Exception {
        String oversizedOrderNo = "O".repeat(65);

        mockMvc.perform(get(DETAIL_URL, oversizedOrderNo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"))
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(orderService);
    }
}
