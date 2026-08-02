package com.example.orderinventory.order.controller;

import com.example.orderinventory.common.result.ApiResult;
import com.example.orderinventory.order.dto.OrderCancelRequest;
import com.example.orderinventory.order.dto.OrderCreateRequest;
import com.example.orderinventory.order.service.OrderService;
import com.example.orderinventory.order.vo.OrderCancelResponse;
import com.example.orderinventory.order.vo.OrderCreateResponse;
import com.example.orderinventory.order.vo.OrderDetailResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请遵循六道之力
 * 【①】这个字段会不会是 null？
 * 【②】这个类需不需要 setter？
 * 【③】有没有统一创建方法？
 * 【④】有没有泛型？
 * 【⑤】有没有依赖具体实现类？
 * 【⑥】传错参数会不会悄悄生成错误结果？
 *
 * @author 我他妈莱纳
 * @date 2026/7/7 21:28
 * @description 类的详细说明
 */
@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResult<OrderCreateResponse> createOrder(
            @Valid  @RequestBody OrderCreateRequest orderCreateRequest){
        OrderCreateResponse response = orderService.createOrder(orderCreateRequest);
        return ApiResult.success(response);
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResult<OrderCancelResponse> cancelOrder(@PathVariable("orderNo") String orderNo,
                                                      @Valid @RequestBody OrderCancelRequest orderCancelRequest){

        OrderCancelResponse response = orderService.cancelOrder(orderNo,orderCancelRequest);
        return ApiResult.success(response);
    }

    @GetMapping("/{orderNo}")
    public ApiResult<OrderDetailResponse> getOrderDetail(
            @PathVariable
            @NotBlank(message = "订单编号不能为空")
            @Size(max = 64, message = "订单编号长度不能超过64") String orderNo)  {
        return ApiResult.success(orderService.getOrderDetail(orderNo));
    }

}

