package com.collaboffice.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification-exchange";
    public static final String NOTIFICATION_RECEIVE_QUEUE = "notification-receive-queue";
    public static final String NOTIFICATION_ROUTING_KEY_PATTERN = "notification.#";
    public static final String NOTIFICATION_PUSH_EXCHANGE = "notification-push-exchange";
    public static final String NOTIFICATION_EMAIL_QUEUE = "notification.email";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange notificationPushExchange() {
        return new FanoutExchange(NOTIFICATION_PUSH_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationReceiveQueue() {
        return QueueBuilder.durable(NOTIFICATION_RECEIVE_QUEUE).build();
    }

    @Bean
    public Binding notificationReceiveBinding(Queue notificationReceiveQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationReceiveQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_ROUTING_KEY_PATTERN);
    }

    @Bean
    public Queue notificationEmailQueue() {
        return QueueBuilder.durable(NOTIFICATION_EMAIL_QUEUE).build();
    }

    @Bean
    public Binding notificationEmailBinding(Queue notificationEmailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationEmailQueue)
                .to(notificationExchange)
                .with("notification.email");
    }

    // ========== Gap-5: 业务事件队列 ==========

    public static final String TASK_DEADLINE_QUEUE = "task.deadline.approaching";
    public static final String MESSAGE_MENTION_QUEUE = "message.mention";
    public static final String MEETING_SOON_QUEUE = "meeting.starting.soon";

    @Bean
    public Queue taskDeadlineQueue() {
        return QueueBuilder.durable(TASK_DEADLINE_QUEUE).build();
    }

    @Bean
    public Binding taskDeadlineBinding(Queue taskDeadlineQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(taskDeadlineQueue)
                .to(notificationExchange)
                .with("task.deadline.approaching");
    }

    @Bean
    public Queue messageMentionQueue() {
        return QueueBuilder.durable(MESSAGE_MENTION_QUEUE).build();
    }

    @Bean
    public Binding messageMentionBinding(Queue messageMentionQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(messageMentionQueue)
                .to(notificationExchange)
                .with("notification.mention");
    }

    @Bean
    public Queue meetingSoonQueue() {
        return QueueBuilder.durable(MEETING_SOON_QUEUE).build();
    }

    @Bean
    public Binding meetingSoonBinding(Queue meetingSoonQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(meetingSoonQueue)
                .to(notificationExchange)
                .with("meeting.starting.soon");
    }
}
