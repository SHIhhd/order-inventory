package com.example.orderinventory.order.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderinventory.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Administrator
* @description 针对表【order_item(订单明细表：保存订单中的商品快照信息)】的数据库操作Mapper
* @createDate 2026-07-01 14:59:51
* @Entity com.example.orderinventory.order.entity.OrderItem
*/
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {


    int batchInsert(@Param("orderItemBatch")List<OrderItem> orderItemBatch);


    //int batchInsert(@Param("orderItemBatch") List<OrderItem> orderItemBatch);
}




