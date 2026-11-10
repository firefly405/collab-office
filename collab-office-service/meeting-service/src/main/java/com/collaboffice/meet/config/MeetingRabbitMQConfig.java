package com.collaboffice.meet.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeetingRabbitMQConfig {

    public static final String INTERACTION_EXCHANGE = "meeting-interaction-exchange";
    public static final String INTERACTION_QUEUE = "meeting.interaction.events";
    public static final String INTERACTION_ROUTING_KEY = "meeting.interaction.#";

    @Bean
    public TopicExchange meetingInteractionExchange() {
        return new TopicExchange(INTERACTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue meetingInteractionQueue() {
        return QueueBuilder.durable(INTERACTION_QUEUE).build();
    }

    @Bean
    public Binding meetingInteractionBinding() {
        return BindingBuilder.bind(meetingInteractionQueue())
                .to(meetingInteractionExchange())
                .with(INTERACTION_ROUTING_KEY);
    }
}
