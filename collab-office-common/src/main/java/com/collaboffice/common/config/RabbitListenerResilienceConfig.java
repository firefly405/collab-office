package com.collaboffice.common.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory")
public class RabbitListenerResilienceConfig {

    @Bean
    public static BeanPostProcessor rabbitListenerRequeueGuardPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof SimpleRabbitListenerContainerFactory factory) {
                    var defaults = new ConditionalRejectingErrorHandler.DefaultExceptionStrategy();
                    factory.setErrorHandler(new ConditionalRejectingErrorHandler(
                            error -> defaults.isFatal(error) || containsSecurityException(error)));
                }
                return bean;
            }
        };
    }

    static boolean containsSecurityException(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SecurityException) {
                return true;
            }
        }
        return false;
    }
}
