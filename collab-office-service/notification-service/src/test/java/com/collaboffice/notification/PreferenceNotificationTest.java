package com.collaboffice.notification;
import com.collaboffice.notification.service.*;
import com.collaboffice.notification.service.impl.NotificationServiceImpl;
import com.collaboffice.notification.mapper.NotificationMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PreferenceNotificationTest {
 @Test void disabledCategoryDoesNotSend(){var jdbc=mock(JdbcTemplate.class);var mapper=mock(NotificationMapper.class);var rabbit=mock(RabbitTemplate.class);when(jdbc.queryForList(anyString(),eq(String.class),eq(1L))).thenReturn(List.of("{\"notifications\":{\"task\":false}}"));new NotificationServiceImpl(mapper,rabbit,jdbc).sendNotification(1L,3,"Task","Changed","t1");verifyNoInteractions(mapper,rabbit);}
 @Test void enabledCategoryPersists(){var jdbc=mock(JdbcTemplate.class);var mapper=mock(NotificationMapper.class);var rabbit=mock(RabbitTemplate.class);when(jdbc.queryForList(anyString(),eq(String.class),eq(1L))).thenReturn(List.of("{\"notifications\":{\"task\":true}}"));new NotificationServiceImpl(mapper,rabbit,jdbc).sendNotification(1L,3,"Task","Changed","t1");verify(mapper).insert(any(com.collaboffice.notification.entity.Notification.class));}
 @Test void subscriptionValidatesTypes(){assertDoesNotThrow(()->SubscriptionService.validate("document","123","daily"));assertThrows(RuntimeException.class,()->SubscriptionService.validate("admin","1","realtime"));assertThrows(RuntimeException.class,()->SubscriptionService.validate("task","1' OR 1=1","realtime"));}
}
