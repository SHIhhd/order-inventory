package com.example.orderinventory.product.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
 * @date 2026/7/5 21:09
 * @description 类的详细说明
 */
public record ProductPageRequest(

    /**
     * 【学习】
     * @Min 和  @Max 不会对 null 进行校验
     */
    @Min(value = 1, message = "pageNo must be greater than or equal to 1")
    Integer pageNo,

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    Integer pageSize,

    String keyword,

    Integer productStatus
) {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 【学习】
     * 未传或传空的分页参数在校验前补齐默认值。
     */
    public ProductPageRequest {
        pageNo = pageNo == null ? DEFAULT_PAGE_NO : pageNo;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    /**
     *  【学习】
     * @AssertTrue 被标注的 boolean 方法必须返回 true，否则参数校验失败。
     * 但是有个缺点就是 ：标在 isXxx 方法上时，isXxx 会被 JavaBean 识别成一个布尔属性。
     * 当前 isProductStatusValid 会形成逻辑属性 productStatusValid
     * TODO 后续需要将注解 @AssertTrue 改成自定义注解
     * @return
     */
    @AssertTrue(message = "productStatus must be 0 or 1")
    public boolean isValidProductStatus() {
        return productStatus == null || productStatus == 0 || productStatus == 1;
    }
}
