package com.example.orderinventory.order.service;

import com.example.orderinventory.order.dto.OrderCreateRequest;
import com.example.orderinventory.order.vo.OrderCreateResponse;

/**
* @author Administrator
* @description 针对表【order_info(订单主表：保存订单整体信息)】的数据库操作Service
* @createDate 2026-07-01 14:59:51
*/
public interface OrderService  {

    OrderCreateResponse createOrder(OrderCreateRequest orderCreateRequest);
}
