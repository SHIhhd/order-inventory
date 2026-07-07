package com.example.orderinventory.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderinventory.stock.entity.StockFlow;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【stock_flow(库存流水表：记录每一次库存变化)】的数据库操作Mapper
* @createDate 2026-07-01 14:49:47
* @Entity com.example.orderinventory.stock.entity.StockFlow
*/
@Mapper
public interface StockFlowMapper extends BaseMapper<StockFlow> {

}




