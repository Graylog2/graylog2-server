/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.notifications;

import com.google.common.collect.Sets;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotificationConfig;
import org.graylog.events.notifications.types.EmailEventNotificationConfig;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.security.encryption.EncryptedValue;
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

    private NotificationDto getHttpNotification(HTTPEventNotificationConfig config) {
        return NotificationDto.builder()
                .title("Foobar")
                .description("")
                .config(config)
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
                        .htmlBodyTemplate("baz")
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
                .htmlBodyTemplate("")
                .build();
        final NotificationDto emptyNotification = getEmailNotification().toBuilder().config(emptyConfig).build();
        final ValidationResult validationResult = emptyNotification.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).hasSize(3);
        assertThat(validationResult.getErrors()).containsOnlyKeys("subject", "body", "recipients");
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
        assertThat(validationResult.getErrors()).isEmpty();
    }

    @Test
    public void testValidateHttpNotificationWithApiKey() {
        HTTPEventNotificationConfig httpConfig = HTTPEventNotificationConfig.Builder.create()
                .url("http://localhost")
                .apiKey("xxx")
                .build();
        NotificationDto notification = getHttpNotification(httpConfig);
        ValidationResult validationResult = notification.validate();
        assertThat(validationResult.getErrors()).containsOnlyKeys("api_secret");

        httpConfig = HTTPEventNotificationConfig.Builder.create()
                .url("http://localhost")
                .apiSecret(EncryptedValue.builder().value("xxx").salt("123").isDeleteValue(false).isKeepValue(false).build())
                .build();
        notification = getHttpNotification(httpConfig);
        validationResult = notification.validate();
        assertThat(validationResult.getErrors()).containsOnlyKeys("api_key");
    }

    @Test
    public void testValidEmailNotification() {
        final NotificationDto validNotification = getEmailNotification();

        final ValidationResult validationResult = validNotification.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors()).isEmpty();
    }

    @Test
    public void testValidLegacyNotification() {
        final NotificationDto validNotification = getLegacyNotification();

        final ValidationResult validationResult = validNotification.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors()).isEmpty();
    }
}
