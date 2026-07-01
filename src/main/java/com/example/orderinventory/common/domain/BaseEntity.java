package com.example.orderinventory.common.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
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
 * @date 2026/6/30 15:39
 * @description
 * 【学习】
 * 因为是公共类，不能实例化，所以使用 abstract 更加符合语义
 * 实现  Serializable  本质就是 让这个对象具备“可以被安全转换成字节流”的能力
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 主键使用包装类
     * long id; 默认值是 0，不是 null。MyBatis-Plus 的 IdType.ASSIGN_ID 通常依赖“主键为空”来生成 ID；
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 实体类中的时间类属性，使用LocalDateTime字段，因为要和数据库交互，参与计算等
     * ApiResult中的时间类属性，使用String，因为不需要跟数据库交互，不参与计算，只是返回给前端，字符串更合适
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 【学习】
     * int OR  Integer？
     * 一、能表达“未设置”
     * int 默认值是 0，你分不清这是业务主动设置的 0，还是根本没赋值。
     *
     * 二、更适配 MyBatis-Plus 自动填充
     * 例如：
     * this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
     * this.strictInsertFill(metaObject, "version", Integer.class, 0);
     * 如果实体字段是 Integer，类型完全一致。
     *
     * 三、更适合数据库语义
     * 数据库字段虽然是 NOT NULL DEFAULT 0，但 Java 入库前仍然可能是 null，然后由自动填充或数据库默认值兜底。
     *
     * 四、避免默认值干扰自动填充
     * int 天生就是 0，某些填充逻辑会认为它已经有值，从而不再填充。
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;

    /**
     * 配合MybatisPlusConfig中的
     * interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor()) 使用
     */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;


}
