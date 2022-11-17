package org.graylog.events.notifications;

import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemNotificationRenderServiceTest {
    static Notification notification;
    static SystemNotificationRenderService renderService;

    @BeforeAll
    static void setup() {
        renderService = new SystemNotificationRenderService();
        notification = new NotificationImpl()
                .addNode("node")
                .addSeverity(Notification.Severity.URGENT)
                .addType(Notification.Type.EMAIL_TRANSPORT_CONFIGURATION_INVALID)
                .addDetail("title", "title")
                .addDetail("description", "description")
                .addDetail("dummy", "dummy")
                .addDetail("exception", new Exception("My Test Exception"))
                .addTimestamp(DateTime.now());
    }

    @Test
    void htmlRenderTestWithReplace() {
        String msg = renderService.renderHtml(notification);
        assertThat(msg).containsSequence("java.lang.Exception: My Test Exception");
        assertThat(msg).containsSequence("<span>");
    }

    @Test
    void plainTextRenderTestWithReplace() {
        String msg = renderService.renderPlainText(notification);
        assertThat(msg).containsSequence("java.lang.Exception: My Test Exception");
        assertThat(msg).doesNotContain("<span>");
    }

    @Disabled
    @Test
    void missingTemplates() {
        for (Notification.Type type : Notification.Type.values()) {
            if (SystemNotificationRenderService.class.getResource(type.toString()) == null) {
                System.out.println("Missing FTL: " + type);
            }
        }
    }
}
