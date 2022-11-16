package org.graylog.events.notifications;

import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemNotificationRenderServiceTest {
    @Test
    public void htmlRenderTest() {
        SystemNotificationRenderService renderService = new SystemNotificationRenderService();

        String msg = renderService.renderHtml("email_transport_configuration_invalid",
                ImmutableMap.of("exception", "eee"));
        assertThat(msg).isNotBlank();
        System.out.println(msg);
    }
}
