package com.collaboffice.doc.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PresentationRabbitMQConfig {

    public static final String PRESENTATION_EXCHANGE = "mindmap-presentation-exchange";

    @Bean
    public TopicExchange mindmapPresentationExchange() {
        return new TopicExchange(PRESENTATION_EXCHANGE, true, false);
    }
}
