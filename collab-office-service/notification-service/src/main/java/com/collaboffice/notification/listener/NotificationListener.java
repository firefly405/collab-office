package com.collaboffice.notification.listener;

import com.collaboffice.notification.client.UserClient;
import com.collaboffice.notification.mapper.UserNotificationPrefMapper;
import com.collaboffice.notification.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final JavaMailSender mailSender;
    private final UserNotificationPrefMapper userNotificationPrefMapper;
    private final UserClient userClient;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.internal-token:default-secret-token}")
    private String internalToken;

    @RabbitListener(queues = "notification-receive-queue")
    public void onReceiveNotification(Map<String, Object> payload,
                                      @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        // The broad notification.# binding also receives mentions owned by BusinessEventListener.
        if ("notification.mention".equals(routingKey)) return;
        log.info("收到MQ业务通知事件: {}", payload);
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj == null) {
                log.warn("业务通知缺少接收人userId，放弃落库推送");
                return;
            }
            Long userId = Long.valueOf(userIdObj.toString());
            Integer type = payload.get("type") != null ? (Integer) payload.get("type") : 1;
            String title = (String) payload.get("title");
            String content = (String) payload.get("content");
            String relatedId = (String) payload.get("relatedId");

            notificationService.sendNotification(userId, type, title, content, relatedId);
        } catch (Exception e) {
            log.error("消费MQ业务通知事件失败: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "notification.email")
    public void onReceiveEmailNotification(Map<String, Object> payload) {
        log.info("收到邮件通知事件: {}", payload);
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj == null) {
                log.warn("邮件通知缺少接收人userId，放弃发送");
                return;
            }
            Long userId = Long.valueOf(userIdObj.toString());

            // 1. Check user preference
            com.collaboffice.notification.entity.UserNotificationPref pref = userNotificationPrefMapper.selectById(userId);
            if (pref != null && pref.getEmailNotify() != null && pref.getEmailNotify() == 0) {
                log.info("用户 {} 已关闭邮件通知，不发送邮件", userId);
                return;
            }

            // 2. Fetch user email
            com.collaboffice.common.result.CommonResult<Map<String, Object>> userResult = userClient.getUserInternal(internalToken, userId);
            if (userResult == null || userResult.getCode() != 200 || userResult.getData() == null) {
                log.warn("无法获取用户 {} 详情，无法发送邮件", userId);
                return;
            }
            Map<String, Object> userData = userResult.getData();
            String email = (String) userData.get("email");
            if (email == null || email.isBlank()) {
                log.warn("用户 {} 邮箱为空，无法发送邮件", userId);
                return;
            }

            String title = (String) payload.getOrDefault("title", "系统提醒");
            String content = (String) payload.getOrDefault("content", "");

            // 3. Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(title);
            
            // Basic HTML template
            String htmlContent = "<html><body>" +
                    "<h2>" + title + "</h2>" +
                    "<p>" + content + "</p>" +
                    "<hr/>" +
                    "<p style='color: #888;'>此邮件为系统自动生成，请勿回复。</p>" +
                    "</body></html>";
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("成功向用户 {} ({}) 发送 HTML 邮件: {}", userId, email, title);

        } catch (Exception e) {
            log.error("处理邮件通知事件失败: {}", e.getMessage(), e);
        }
    }
}
