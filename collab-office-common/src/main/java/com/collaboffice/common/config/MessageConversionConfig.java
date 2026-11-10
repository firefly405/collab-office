package com.collaboffice.common.config;

import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConditionalOnClass(name="org.springframework.amqp.rabbit.core.RabbitTemplate")
public class MessageConversionConfig {
    /**
     * 业务事件消息转换器。
     *
     * <p>白名单必须同时包含集合的<b>实现类</b>和它们的<b>序列化代理类</b>。
     * {@code List.of()} / {@code Map.of()} / {@code Set.of()} 返回的不可变集合在 Java
     * 序列化时并不会写出实现类名，而是写出统一的代理类 {@code java.util.CollSer}
     * （由 {@code ImmutableCollections} 的 {@code writeReplace} 产生）。白名单缺这一项时，
     * 消费端反序列化会抛 {@code SecurityException}；由于异常发生在监听方法执行之前，
     * 方法体内的 try/catch 捕获不到，Spring AMQP 默认 requeue，消息退回队列后被立刻
     * 重新投递，形成无限重试。该故障曾把 notification-service.out.log 写到 117GB。</p>
     *
     * <p>这里只放行 JDK 标准集合类型，不接受任意业务类，反序列化白名单的安全边界仍然成立。</p>
     */
    @Bean
    public SimpleMessageConverter businessMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of(
                "java.util.HashMap",
                "java.util.LinkedHashMap",
                "java.util.ArrayList",
                "java.util.HashSet",
                "java.util.LinkedHashSet",
                "java.util.Arrays$ArrayList",
                // List.of/Map.of/Set.of 的序列化代理类，缺此项会触发消费端无限重试
                "java.util.CollSer"));
        return converter;
    }
}
