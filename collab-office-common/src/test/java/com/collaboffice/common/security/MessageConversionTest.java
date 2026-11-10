package com.collaboffice.common.security;
import com.collaboffice.common.config.MessageConversionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MessageConversionTest {
    @Test void allowsBusinessMapAndMemberList() {
        var converter=new MessageConversionConfig().businessMessageConverter();
        var payload=new LinkedHashMap<String,Object>();
        payload.put("content","hello"); payload.put("memberIds",new ArrayList<>(List.of(1L,2L)));
        assertEquals(payload,converter.fromMessage(converter.toMessage(payload,new MessageProperties())));
    }
    @Test void rejectsOtherSerializableTypes() {
        var converter=new MessageConversionConfig().businessMessageConverter();
        var message=converter.toMessage(new java.io.File("local-only"),new MessageProperties());
        assertThrows(SecurityException.class,()->converter.fromMessage(message));
    }

    /**
     * 回归：Map.of/List.of/Set.of 这类不可变集合在 Java 序列化时会写出代理类
     * java.util.CollSer。若白名单缺少该类，消费端反序列化会抛 SecurityException，
     * 消息被重新入队后无限重试（曾把 notification-service.out.log 写到 117GB）。
     */
    @Test void allowsImmutableCollectionsFromMapOfAndListOf() {
        var converter=new MessageConversionConfig().businessMessageConverter();
        var payload=Map.<String,Object>of(
                "userId",7L,
                "title","文档评论提及你",
                "content","Alice：请看这里",
                "relatedId","doc-1",
                "userIds",List.of(1L,2L));
        var message=converter.toMessage(payload,new MessageProperties());
        assertDoesNotThrow(()->converter.fromMessage(message));
        assertEquals(payload,converter.fromMessage(message));
    }

    @Test void allowsNestedSetOfAndEmptyImmutableCollections() {
        var converter=new MessageConversionConfig().businessMessageConverter();
        var payload=new LinkedHashMap<String,Object>();
        payload.put("options",Set.of("方案A","方案B"));
        payload.put("emptyList",List.of());
        payload.put("emptyMap",Map.of());
        var message=converter.toMessage(payload,new MessageProperties());
        assertDoesNotThrow(()->converter.fromMessage(message));
    }
}
