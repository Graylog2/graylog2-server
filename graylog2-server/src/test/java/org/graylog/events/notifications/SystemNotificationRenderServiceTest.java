package org.graylog.events.notifications;

import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog.events.processor.systemnotification.TemplateRenderResponse;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemNotificationRenderServiceTest {
    static NotificationService notificationService = mock(NotificationService.class);
    static org.graylog2.Configuration graylogConfig = mock(org.graylog2.Configuration.class);
    static SystemNotificationRenderService renderService;
    Notification notification;

    @BeforeAll
    static void setup() {
        renderService = new SystemNotificationRenderService(notificationService, graylogConfig);
    }

    @Test
    void htmlRenderTestWithReplace() {
        notification = new NotificationImpl()
                .addNode("node")
                .addSeverity(Notification.Severity.URGENT)
                .addType(Notification.Type.EMAIL_TRANSPORT_CONFIGURATION_INVALID)
                .addDetail("title", "title")
                .addDetail("description", "description")
                .addDetail("dummy", "dummy")
                .addDetail("exception", new Exception("My Test Exception"))
                .addTimestamp(DateTime.now());
        when(notificationService.getByType(any())).thenReturn(Optional.of(notification));

        TemplateRenderResponse renderResponse = renderService.renderHtml(notification);
        assertThat(renderResponse.title()).isEqualToIgnoringWhitespace("Email Transport Configuration is missing or invalid!");
        assertThat(renderResponse.description()).containsSequence("java.lang.Exception: My Test Exception");
        assertThat(renderResponse.description()).containsSequence("<span>");
    }

    @Disabled
    @Test
    void missingTemplates() {
        for (Notification.Type type : Notification.Type.values()) {
            if (SystemNotificationRenderService.class.getResource(type.toString().toLowerCase()) == null) {
                System.out.println("Missing FTL: " + type);
            }
        }
    }
}
