package com.example.orderinventory.product.vo;

import com.example.orderinventory.product.entity.Product;
import lombok.Getter;

import java.time.LocalDateTime;

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
 * @date 2026/7/1 20:56
 * @description 类的详细说明
 */
@Getter
public class ProductVO  {


    private ProductVO(Product product) {
        this.id = product.getId();
        this.skuCode = product.getSkuCode();
        this.productName = product.getProductName();
        this.productStatus = product.getProductStatus();
        this.salePrice = product.getSalePrice();
        this.remark = product.getRemark();
        this.createTime = product.getCreateTime();
        this.updateTime = product.getUpdateTime();
    }

    private Long id;
    /**
     * 商品SKU编码，业务唯一标识
     */
    private String skuCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品状态：0-下架，1-上架
     */
    private Integer productStatus;

    /**
     * 销售单价，单位：分
     */
    private Long salePrice;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static ProductVO from(Product product) {
        return new ProductVO(product);
    }

}
