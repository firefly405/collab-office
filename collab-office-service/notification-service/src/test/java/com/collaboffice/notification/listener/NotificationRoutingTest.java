package com.collaboffice.notification.listener;

import com.collaboffice.notification.client.UserClient;
import com.collaboffice.notification.mapper.UserNotificationPrefMapper;
import com.collaboffice.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.mockito.Mockito.*;

class NotificationRoutingTest {
    private final NotificationService service = mock(NotificationService.class);
    private final NotificationListener generic = new NotificationListener(service,
            mock(JavaMailSender.class), mock(UserNotificationPrefMapper.class), mock(UserClient.class));

    @Test
    void mentionDeliveredToBothQueuesCreatesOneNotification() {
        Map<String,Object> payload = Map.of("userId", 1L, "title", "Mention", "content", "Review", "relatedId", "doc");
        generic.onReceiveNotification(payload, "notification.mention");
        new BusinessEventListener(service).onMessageMention(payload);
        verify(service).sendNotification(1L, 1, "Mention", "Review", "doc");
        verifyNoMoreInteractions(service);
    }

    @Test
    void meetingStillUsesGenericConsumer() {
        generic.onReceiveNotification(Map.of("userId", 2L, "type", 4, "title", "Meeting",
                "content", "Invitation", "relatedId", "room"), "notification.meeting");
        verify(service).sendNotification(2L, 4, "Meeting", "Invitation", "room");
        verifyNoMoreInteractions(service);
    }
}
