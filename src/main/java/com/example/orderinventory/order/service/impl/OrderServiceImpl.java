package com.example.orderinventory.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.orderinventory.order.entity.OrderInfo;
import com.example.orderinventory.order.service.OrderService;
import com.example.orderinventory.order.mapper.OrderInfoMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【order_info(订单主表：保存订单整体信息)】的数据库操作Service实现
* @createDate 2026-07-01 14:59:51
*/
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
    implements OrderService {

}




