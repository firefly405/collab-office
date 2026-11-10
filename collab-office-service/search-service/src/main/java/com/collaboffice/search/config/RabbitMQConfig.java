package com.collaboffice.search.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INDEX_EXCHANGE = "search-index-exchange";
    public static final String DOCUMENT_QUEUE = "search-document-queue";
    public static final String TASK_QUEUE = "search-task-queue";

    public SimpleMessageConverter indexMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        // Existing document/task producers serialize HashMap events.
        converter.setAllowedListPatterns(java.util.List.of("java.util.HashMap"));
        return converter;
    }
    
    @Bean
    public TopicExchange indexExchange() {
        return new TopicExchange(INDEX_EXCHANGE, true, false);
    }

    @Bean
    public Queue documentQueue() {
        return QueueBuilder.durable(DOCUMENT_QUEUE).build();
    }

    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(TASK_QUEUE).build();
    }

    @Bean
    public Binding documentBinding(Queue documentQueue, TopicExchange indexExchange) {
        return BindingBuilder.bind(documentQueue).to(indexExchange).with("search.document.#");
    }

    @Bean
    public Binding taskBinding(Queue taskQueue, TopicExchange indexExchange) {
        return BindingBuilder.bind(taskQueue).to(indexExchange).with("search.task.#");
    }
}
