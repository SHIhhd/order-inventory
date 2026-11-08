package com.example.orderinventory.stock.dto;

import com.example.orderinventory.stock.enums.StockFlowBizType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

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
 * @date 2026/8/2 17:41
 * @description 类的详细说明
 */
public record StockFlowPageRequest(

        @Min(value = 1 ,message = "pageNo must be greater than or equal to 1")
        Integer pageNo,

        @Min(value = 1, message = "pageSize must be greater than or equal to 1")
        @Max(value = 100, message = "pageSize must be less than or equal to 100")
        Integer pageSize,

        Long productId,

        String skuCode,

        String bizNo,

        Integer bizType,

        /**
         * 【学习】
         * 日期类校验
         */
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime
) {
    /**
     * 【学习】
     * param参数接收，如何传入默认值
     */
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 【学习】
     * 如何在null的情况下忽略
     * 而在纯空白的情况下校验
     * 还有一种方法可以参考
     * 在对应的变量上加上
     *     @Pattern(regexp = ".*\\S.*", message = "bizNo 不能是空字符串或纯空白字符")
     *     表示：参数可以不传，但只要传了，就不能是空字符串或纯空白字符，否则返回 HTTP 400。
     *     但这样 首尾带空白 也会通过校验：[   SKU-001  ]
     *     如果使用注解，那么在构造器上需要加上
     *     public StockFlowPageRequest {
     *         if (skuCode != null) {
     *             skuCode = skuCode.strip();
     *         }
     *
     *         if (bizNo != null) {
     *             bizNo = bizNo.strip();
     *         }
     *     }
     */
    public StockFlowPageRequest {
        pageNo = pageNo == null ? DEFAULT_PAGE_NO : pageNo;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        skuCode = normalizeQueryText(skuCode);
        bizNo = normalizeQueryText(bizNo);
    }

    /**
     * 【学习】
     * .strip() 删除首位空白
     * [   SKU-001  ]通过.strip() 转变为 [SKU-001]
     * @param value
     * @return
     */
    private static String normalizeQueryText(String value) {
        //空或者空白都返回空
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.strip();
    }
    @AssertTrue(message = "bizType不合法")
    public boolean isValidBizType() {
        return bizType == null
                || StockFlowBizType.findByCode(bizType).isPresent();
    }

    @AssertTrue(message = "startTime不能晚于endTime")
    public boolean isValidTimeRange() {
        // 时间参数是可选的，只传一个也允许
        if (startTime == null && endTime == null) {
            return true;
        }
        if(startTime == null || endTime == null){
            return false;
        }
        return !startTime.isAfter(endTime);
    }
}
