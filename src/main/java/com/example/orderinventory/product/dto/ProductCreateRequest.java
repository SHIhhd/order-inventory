package com.example.orderinventory.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
 * @date 2026/7/1 20:51
 * @description 类的详细说明
 */
@Data
public class ProductCreateRequest {
    /**
     * 商品SKU编码，业务唯一标识
     */

    @NotBlank(message = "商品SKU编码不能为空")
    @Size(max = 64 ,message = "商品SKU编码长度大于1小于64")
    private String skuCode;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    @Size( max = 128 ,message = "商品名称长度大于1小于128")
    private String productName;


    @NotNull
    @Min(value = 0 , message = "商品状态只能是0或1")
    @Max(value = 1 , message = "商品状态只能是0或1")
    private Integer productStatus;

    /**
     * 销售单价，单位：分
     */
    @NotNull
    @Positive(message = "销售单价需大于0")
    private Long salePrice;

    /**
     * 备注
     */
    @Size(max = 512, message = "备注长度不能超过512")
    private String remark;

}
