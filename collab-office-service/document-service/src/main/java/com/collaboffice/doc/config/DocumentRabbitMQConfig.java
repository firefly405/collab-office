package com.collaboffice.doc.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentRabbitMQConfig {

    public static final String TOPIC_EXCHANGE = "doc-operation-exchange";
    public static final String PERSIST_QUEUE = "doc-persist-queue";
    public static final String PERSIST_KEY = "doc.operation.persist";
    public static final String DLX_EXCHANGE = "doc-operation-dlx";
    public static final String DLQ_QUEUE = "doc-persist-dlq";
    public static final String DLQ_KEY = "doc.operation.persist.dlq";

    @Bean
    public TopicExchange docOperationExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange docOperationDlx() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue docPersistQueue() {
        return QueueBuilder.durable(PERSIST_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLQ_KEY)
                .build();
    }

    @Bean
    public Queue docPersistDlq() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding persistBinding() {
        return BindingBuilder.bind(docPersistQueue())
                .to(docOperationExchange())
                .with(PERSIST_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(docPersistDlq())
                .to(docOperationDlx())
                .with(DLQ_KEY);
    }
}
