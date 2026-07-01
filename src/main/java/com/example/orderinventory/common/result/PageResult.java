package com.example.orderinventory.common.result;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Page response structure.
 *
 * <p>字段类型选择说明：
 * pageNo、pageSize、total、pages 是分页响应中应始终返回的字段，所以这里使用
 * int/long 基本类型，避免接口返回 null。请求 DTO 中的分页参数可以使用 Integer，
 * 因为请求参数可能存在“用户未传”的情况；响应 DTO 则应尽量给出明确值。</p>
 *
 * <p>records 使用 List<T> 而不是 ArrayList<T>：
 * 对外暴露接口时依赖抽象类型 List，可以兼容 ArrayList、Collections.emptyList()
 * 以及其他 List 实现，减少调用方和具体实现类的耦合。</p>
 *
 * @author Administrator
 * @param <T> record data type
 */
@Getter
public final class PageResult<T> {

    private final List<T> records;

    private final int pageNo;

    private final int pageSize;

    private final long total;

    private final long pages;

    /**
     * validate(pageNo, pageSize, total, pages) 这段代码太优秀了，解耦！【学习】了！
     * records == null ? Collections.emptyList() : List.copyOf(records);
     * 太优秀了，①用来兜底 ②构造时做防御性拷贝，不直接传入引用，【学习】了！
     *
     * @param records
     * @param pageNo
     * @param pageSize
     * @param total
     * @param pages
     */
    private PageResult(List<T> records, int pageNo, int pageSize, long total, long pages) {
        validate(pageNo, pageSize, total, pages);
        this.records = records == null ? Collections.emptyList() : List.copyOf(records);
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = pages;
    }

    public static <T> PageResult<T> of(List<T> records, int pageNo, int pageSize, long total) {
        long pages = calculatePages(total, pageSize);
        return new PageResult<>(records, pageNo, pageSize, total, pages);
    }

//    public static <T> PageResult<T> of(List<T> records, int pageNo, int pageSize, long total, long pages) {
//        return new PageResult<>(records, pageNo, pageSize, total, pages);
//    }

    public static <T> PageResult<T> empty(int pageNo, int pageSize) {
        return new PageResult<>(Collections.emptyList(), pageNo, pageSize, 0L, 0L);
    }

    private static void validate(int pageNo, int pageSize, long total, long pages) {
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than or equal to 1");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must be greater than or equal to 0");
        }
        if (pages < 0) {
            throw new IllegalArgumentException("pages must be greater than or equal to 0");
        }
    }

    /**
     * 使用下面方法计算页码，如果total 接近 Long.MAX_VALUE 时，加法会溢出成负数
     * (total + pageSize - 1) / pageSize
     * 所以需要调整为
     * total / pageSize + (total % pageSize == 0 ? 0 : 1)
     * @param total
     * @param pageSize
     * @return
     */
    private static long calculatePages(long total, int pageSize) {
        if (total <= 0 || pageSize <= 0) {
            return 0L;
        }
        return total / pageSize + (total % pageSize == 0 ? 0 : 1);
    }
}
