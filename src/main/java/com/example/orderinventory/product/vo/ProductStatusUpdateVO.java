package com.example.orderinventory.product.vo;

import lombok.Getter;

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
 * @date 2026/7/6 10:49
 * @description 类的详细说明
 */
@Getter
public class ProductStatusUpdateVO {

    private ProductStatusUpdateVO(Long id, Integer productStatus) {
        this.id = id;
        this.productStatus = productStatus;
    }

    private Long id;

    private Integer productStatus;

    public static ProductStatusUpdateVO of(Long id, Integer productStatus){
        return new ProductStatusUpdateVO(id,productStatus);
    }
}
