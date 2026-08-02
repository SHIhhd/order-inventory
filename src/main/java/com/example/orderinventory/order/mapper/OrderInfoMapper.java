package com.example.orderinventory.order.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderinventory.order.entity.OrderInfo;
import com.example.orderinventory.order.mapper.model.OrderDetailQueryResult;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
* @author Administrator
* @description 针对表【order_info(订单主表：保存订单整体信息)】的数据库操作Mapper
* @createDate 2026-07-01 14:59:51
* @Entity com.example.orderinventory.order.entity.OrderInfo
*/
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 【学习】
     * 这里的入参如何使用实体类，那么xml中的sql中where的条件orderstatus
     * 就要使用魔法是 where order_status = 10 ,可扩展性就差了点
     *
     *         set order_status=#{targetStatus,jdbcType=VARCHAR},
     *             cancel_reason=#{cancelReason,jdbcType=VARCHAR},
     *             update_time=#{updateTime,jdbcType=TIMESTAMP},
     *             version=#{version,jdbcType=INTEGER} + 1
     *         where order_no = #{orderNo,jdbcType=VARCHAR}
     *         and order_status =  #{expectedStatus,jdbcType=VARCHAR}
     *     </update>
     * @param
     * @return
     */
    int cancelCreatedOrder(@Param("orderNo") String orderNo,
                           @Param("targetStatus")Integer targetStatus,
                           @Param("expectedStatus")Integer expectedStatus,
                           @Param("cancelReason")String cancelReason,
                           @Param("updateTime")LocalDateTime now);

    OrderDetailQueryResult selectOrderDetailByOrderNo(@Param("orderNo")String orderNo);





}




