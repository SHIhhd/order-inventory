package com.example.orderinventory.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

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
 * @date 2026/7/15 15:23
 * @description 类的详细说明
 * 定制 Spring Boot 使用的 Jackson，让 Java 对象与 JSON 之间的转换遵守项目约定。
 */
@Configuration
public class JacksonDateTimeConfig {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            //序列化
            builder.serializers(
                    new LocalDateTimeSerializer(DATE_TIME_FORMATTER)
            );
            builder.featuresToDisable(
                    DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            //反序列化
            builder.deserializers(
                    new LocalDateTimeDeserializer(DATE_TIME_FORMATTER)
            );
        };
    }
}