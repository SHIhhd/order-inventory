package com.example.orderinventory.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus base configuration.
 */
@Configuration
@MapperScan("com.example.orderinventory.*.mapper")
public class MybatisPlusConfig {

    /**
     * Register MyBatis-Plus interceptors.
     *
     * <p>PaginationInnerInterceptor is required when using MyBatis-Plus Page
     * queries. DbType.MYSQL lets MyBatis-Plus generate MySQL-compatible paging
     * SQL with LIMIT/OFFSET.</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件：让 @Version 生效
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 分页插件：支持 MyBatis-Plus 的 Page 分页查询，并按 MySQL 方言生成分页 SQL。
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setOverflow(false);
        paginationInnerInterceptor.setMaxLimit(100L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }
}
