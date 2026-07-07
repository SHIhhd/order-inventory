package com.example.orderinventory.common.exception;

import com.example.orderinventory.common.result.ErrorCode;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

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
 * @date 2026/7/7 14:15
 * @description 类的详细说明
 */
public class BusinessAssert {

    public static void notNull(@Nullable Object obj  ,String message){

        if(obj == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR,message);
        }

    }

    public static void notNull(@Nullable Object obj  ,ErrorCode errorcode,String message){

        if(obj == null){
            throw new BusinessException(errorcode,message);
        }

    }

    public static void hasText(@Nullable String str, String message){
        if(!StringUtils.hasText(str)){
            throw new BusinessException(ErrorCode.PARAM_ERROR,message);
        }
    }
}
