package com.collaboffice.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitListenerResilienceConfigTest {

    @Test
    void findsNestedSecurityExceptionOnly() {
        assertTrue(RabbitListenerResilienceConfig.containsSecurityException(
                new RuntimeException(new SecurityException("unauthorized class"))));
        assertFalse(RabbitListenerResilienceConfig.containsSecurityException(
                new RuntimeException(new IllegalStateException("temporary failure"))));
    }
}
