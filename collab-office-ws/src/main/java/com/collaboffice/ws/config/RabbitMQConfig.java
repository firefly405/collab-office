package com.collaboffice.ws.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CHAT_EXCHANGE = "chat-message-exchange";
    public static final String CHAT_BROADCAST_KEY = "chat.message.broadcast";
    public static final String NOTIFICATION_PUSH_EXCHANGE = "notification-push-exchange";

    @Bean
    public TopicExchange chatMessageExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange notificationPushExchange() {
        return new FanoutExchange(NOTIFICATION_PUSH_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatTempQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue notificationTempQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding chatBinding(Queue chatTempQueue, TopicExchange chatMessageExchange) {
        return BindingBuilder.bind(chatTempQueue)
                .to(chatMessageExchange)
                .with(CHAT_BROADCAST_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationTempQueue, FanoutExchange notificationPushExchange) {
        return BindingBuilder.bind(notificationTempQueue)
                .to(notificationPushExchange);
    }
}
