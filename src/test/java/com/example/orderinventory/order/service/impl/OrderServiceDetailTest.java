package com.example.orderinventory.order.service.impl;

import com.example.orderinventory.common.exception.BusinessException;
import com.example.orderinventory.common.result.ErrorCode;
import com.example.orderinventory.order.mapper.OrderInfoMapper;
import com.example.orderinventory.order.mapper.OrderItemMapper;
import com.example.orderinventory.order.mapper.model.OrderDetailItemQueryResult;
import com.example.orderinventory.order.mapper.model.OrderDetailQueryResult;
import com.example.orderinventory.order.vo.OrderDetailResponse;
import com.example.orderinventory.product.mapper.ProductMapper;
import com.example.orderinventory.stock.mapper.ProductStockMapper;
import com.example.orderinventory.stock.mapper.StockFlowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceDetailTest {

    private static final String ORDER_NO = "ORD202606291530201987654321";

    private OrderInfoMapper orderInfoMapper;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderInfoMapper = mock(OrderInfoMapper.class);
        orderService = new OrderServiceImpl(
                mock(ProductMapper.class),
                mock(StockFlowMapper.class),
                mock(ProductStockMapper.class),
                orderInfoMapper,
                mock(OrderItemMapper.class)
        );
    }

    @Test
    void getOrderDetailShouldMapQueryResultToResponse() {
        OrderDetailQueryResult queryResult = createQueryResult(10, List.of(createItem()));
        when(orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO)).thenReturn(queryResult);

        OrderDetailResponse response = orderService.getOrderDetail(ORDER_NO);

        assertThat(response.orderNo()).isEqualTo(ORDER_NO);
        assertThat(response.buyerId()).isEqualTo(10001L);
        assertThat(response.orderStatus()).isEqualTo(10);
        assertThat(response.orderStatusName()).isEqualTo("已创建");
        assertThat(response.totalAmount()).isEqualTo(1_199_800L);
        assertThat(response.totalQuantity()).isEqualTo(2);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).skuCode()).isEqualTo("SKU-APPLE-001");
        assertThatThrownBy(() -> response.items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        verify(orderInfoMapper).selectOrderDetailByOrderNo(ORDER_NO);
    }

    @Test
    void getOrderDetailShouldReturnImmutableEmptyItems() {
        OrderDetailQueryResult queryResult = createQueryResult(20, List.of());
        when(orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO)).thenReturn(queryResult);

        OrderDetailResponse response = orderService.getOrderDetail(ORDER_NO);

        assertThat(response.orderStatusName()).isEqualTo("已取消");
        assertThat(response.items()).isEmpty();
        assertThatThrownBy(() -> response.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getOrderDetailShouldThrowOrderNotFoundWhenMapperReturnsNull() {
        when(orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO)).thenReturn(null);

        assertThatThrownBy(() -> orderService.getOrderDetail(ORDER_NO))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }

    @Test
    void getOrderDetailShouldRejectUnknownOrderStatus() {
        OrderDetailQueryResult queryResult = createQueryResult(99, List.of());
        when(orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO)).thenReturn(queryResult);

        assertThatThrownBy(() -> orderService.getOrderDetail(ORDER_NO))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SYSTEM_ERROR);
                    assertThat(exception.getMessage()).contains("orderStatus=99");
                });
    }

    @Test
    void getOrderDetailShouldRejectNullOrderStatus() {
        OrderDetailQueryResult queryResult = createQueryResult(null, List.of());
        when(orderInfoMapper.selectOrderDetailByOrderNo(ORDER_NO)).thenReturn(queryResult);

        assertThatThrownBy(() -> orderService.getOrderDetail(ORDER_NO))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SYSTEM_ERROR));
    }

    private OrderDetailQueryResult createQueryResult(
            Integer status,
            List<OrderDetailItemQueryResult> items) {
        LocalDateTime time = LocalDateTime.of(2026, 6, 29, 15, 30, 20);
        OrderDetailQueryResult result = new OrderDetailQueryResult();
        result.setOrderNo(ORDER_NO);
        result.setBuyerId(10001L);
        result.setOrderStatus(status);
        result.setTotalAmount(1_199_800L);
        result.setTotalQuantity(2);
        result.setRemark("测试下单");
        result.setCreateTime(time);
        result.setUpdateTime(time);
        result.setItems(items);
        return result;
    }

    private OrderDetailItemQueryResult createItem() {
        OrderDetailItemQueryResult item = new OrderDetailItemQueryResult();
        item.setProductId(1_987_654_321_000_000_001L);
        item.setSkuCode("SKU-APPLE-001");
        item.setProductName("苹果手机");
        item.setSalePrice(599_900L);
        item.setQuantity(2);
        item.setItemAmount(1_199_800L);
        return item;
    }
}
