package com.example.orderinventory.stock.vo;

import com.example.orderinventory.stock.entity.ProductStock;
import lombok.Data;

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
 * @date 2026/7/7 11:04
 * @description 类的详细说明
 */
@Data
public class ProductStockVO {

    private ProductStockVO(ProductStock productStock){
        this.productId = productStock.getProductId();
        this.availableQuantity = productStock.getAvailableQuantity();
        this.lockedQuantity = productStock.getLockedQuantity();
        this.totalInQuantity = productStock.getTotalInQuantity();
        this.totalOutQuantity = productStock.getTotalOutQuantity();
    }

    private Long productId;

    private Integer availableQuantity;

    private Integer lockedQuantity;

    private Integer totalInQuantity;

    private Integer totalOutQuantity;

    public static ProductStockVO from (ProductStock productStock){
        return new ProductStockVO(productStock);
    }

}
