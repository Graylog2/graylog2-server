package org.graylog.events.notifications;

import com.google.common.collect.Sets;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotificationConfig;
import org.graylog.events.notifications.types.EmailEventNotificationConfig;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog2.plugin.rest.ValidationResult;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationDtoTest {
    private NotificationDto getHttpNotification() {
        return NotificationDto.builder()
                .title("Foobar")
                .description("")
                .config(HTTPEventNotificationConfig.Builder.create()
                        .url("http://localhost")
                        .build())
                .build();
    }

    private NotificationDto getEmailNotification() {
        return NotificationDto.builder()
                .title("Foobar")
                .description("")
                .config(EmailEventNotificationConfig.Builder.create()
                        .sender("foo@graylog.org")
                        .subject("foo")
                        .bodyTemplate("bar")
                        .emailRecipients(Sets.newHashSet("foo@graylog.org"))
                        .build())
                .build();
    }

    private NotificationDto getLegacyNotification() {
        return NotificationDto.builder()
                .title("Foobar")
                .description("")
                .config(LegacyAlarmCallbackEventNotificationConfig.Builder.create()
                        .callbackType("foo")
                        .configuration(new HashMap<>())
                        .build())
                .build();
    }

    @Test
    public void testValidateWithEmptyTitle() {
        final NotificationDto invalidNotification = getHttpNotification().toBuilder().title("").build();
        final ValidationResult validationResult = invalidNotification.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("title");
    }

    @Test
    public void testValidateWithEmptyConfig() {
        final NotificationDto invalidNotification = NotificationDto.builder()
                .title("Foo")
                .description("")
                .config(new EventNotificationConfig.FallbackNotificationConfig())
                .build();
        final ValidationResult validationResult = invalidNotification.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("config");
    }

    @Test
    public void testValidateHttpWithEmptyConfigParameters() {
        final HTTPEventNotificationConfig emptyConfig = HTTPEventNotificationConfig.Builder.create()
                .url("")
                .build();
        final NotificationDto emptyNotification = getHttpNotification().toBuilder().config(emptyConfig).build();
        final ValidationResult validationResult = emptyNotification.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("url");
    }

    @Test
    public void testValidateEmailWithEmptyConfigParameters() {
        final EmailEventNotificationConfig emptyConfig = EmailEventNotificationConfig.Builder.create()
                .sender("")
                .subject("")
                .bodyTemplate("")
                .build();
        final NotificationDto emptyNotification = getEmailNotification().toBuilder().config(emptyConfig).build();
        final ValidationResult validationResult = emptyNotification.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors().size()).isEqualTo(4);
        assertThat(validationResult.getErrors()).containsOnlyKeys("subject", "sender", "body_template", "recipients");
    }

    @Test
    public void testValidateLegacyWithEmptyConfigParameters() {
        final LegacyAlarmCallbackEventNotificationConfig emptyConfig = LegacyAlarmCallbackEventNotificationConfig.Builder.create()
                .callbackType("")
                .configuration(new HashMap<>())
                .build();
        final NotificationDto emptyNotification = getLegacyNotification().toBuilder().config(emptyConfig).build();
        final ValidationResult validationResult = emptyNotification.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("callback_type");
    }

    @Test
    public void testValidHttpNotification() {
        final NotificationDto validNotification = getHttpNotification();

        final ValidationResult validationResult = validNotification.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void testValidEmailNotification() {
        final NotificationDto validNotification = getEmailNotification();

        final ValidationResult validationResult = validNotification.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void testValidLegacyNotification() {
        final NotificationDto validNotification = getLegacyNotification();

        final ValidationResult validationResult = validNotification.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }
}
