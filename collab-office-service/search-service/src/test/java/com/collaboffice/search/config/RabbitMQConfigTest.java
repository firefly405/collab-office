package com.collaboffice.search.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RabbitMQConfigTest {
    private final SimpleMessageConverter producer = new SimpleMessageConverter();
    private final SimpleMessageConverter consumer = new RabbitMQConfig().indexMessageConverter();

    @Test
    void consumesExistingDocumentAndTaskEvents() {
        Map<String, Object> event = new HashMap<>();
        event.put("id", "2110586858785345536");
        event.put("title", "Welcome");
        event.put("spaceId", 1L);
        event.put("status", 0);
        event.put("description", null);
        Message message = producer.toMessage(event, new MessageProperties());

        assertEquals(event, consumer.fromMessage(message));
    }

    @Test
    void stillRejectsUnapprovedSerializedClasses() {
        Map<String, Object> event = new HashMap<>();
        event.put("unexpected", new Date());
        Message message = producer.toMessage(event, new MessageProperties());

        assertThrows(SecurityException.class, () -> consumer.fromMessage(message));
    }
}
