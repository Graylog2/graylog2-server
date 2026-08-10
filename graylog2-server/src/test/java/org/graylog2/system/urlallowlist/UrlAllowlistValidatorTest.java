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
package org.graylog2.system.urlallowlist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.NotificationTestData;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.integrations.notifications.types.SlackEventNotificationConfig;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UrlAllowlistValidatorTest {

    @Mock
    private UrlAllowlistService allowlistService;

    @Mock
    private UrlAllowlistNotificationService allowlistNotificationService;

    private UrlAllowlistValidator validator;

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/test";

    @BeforeEach
    void setUp() {
        validator = new UrlAllowlistValidator(allowlistService, allowlistNotificationService);
    }

    @Test
    void doesNothingWhenUrlIsAllowlisted() {
        when(allowlistService.isAllowlisted(WEBHOOK_URL)).thenReturn(true);

        assertThatCode(() -> validator.validateUrl(WEBHOOK_URL, buildContext("notif-1")))
                .doesNotThrowAnyException();

        verify(allowlistNotificationService, never()).publishAllowlistFailure(anyString());
    }

    @Test
    void warnsWhenNotAllowlistedAndEnforceDisabled() {
        when(allowlistService.isAllowlisted(WEBHOOK_URL)).thenReturn(false);
        when(allowlistService.getAllowlist()).thenReturn(allowlistWithEnforce(false));

        assertThatCode(() -> validator.validateUrl(WEBHOOK_URL, buildContext("notif-1")))
                .doesNotThrowAnyException();

        verify(allowlistNotificationService).publishAllowlistFailure(anyString());
    }

    @Test
    void throwsPermanentExceptionWhenEnforceEnabled() {
        when(allowlistService.isAllowlisted(WEBHOOK_URL)).thenReturn(false);
        when(allowlistService.getAllowlist()).thenReturn(allowlistWithEnforce(true));

        assertThatThrownBy(() -> validator.validateUrl(WEBHOOK_URL, buildContext("notif-1")))
                .isInstanceOf(PermanentEventNotificationException.class)
                .hasMessageContaining(WEBHOOK_URL);

        verify(allowlistNotificationService).publishAllowlistFailure(anyString());
    }

    @Test
    void skipsSystemNotificationForTestNotifications() {
        when(allowlistService.isAllowlisted(WEBHOOK_URL)).thenReturn(false);
        when(allowlistService.getAllowlist()).thenReturn(allowlistWithEnforce(false));

        assertThatCode(() -> validator.validateUrl(WEBHOOK_URL,
                buildContext(NotificationTestData.TEST_NOTIFICATION_ID)))
                .doesNotThrowAnyException();

        verify(allowlistNotificationService, never()).publishAllowlistFailure(anyString());
    }

    @Test
    void throwsButSkipsSystemNotificationForTestNotificationsWithEnforce() {
        when(allowlistService.isAllowlisted(WEBHOOK_URL)).thenReturn(false);
        when(allowlistService.getAllowlist()).thenReturn(allowlistWithEnforce(true));

        assertThatThrownBy(() -> validator.validateUrl(WEBHOOK_URL,
                buildContext(NotificationTestData.TEST_NOTIFICATION_ID)))
                .isInstanceOf(PermanentEventNotificationException.class);

        verify(allowlistNotificationService, never()).publishAllowlistFailure(anyString());
    }

    private static UrlAllowlist allowlistWithEnforce(boolean enforce) {
        return UrlAllowlist.create(Collections.emptyList(), false, enforce);
    }

    private static EventNotificationContext buildContext(String notificationId) {
        final EventDto eventDto = EventDto.builder()
                .alert(true)
                .eventDefinitionId("event-def-1")
                .eventDefinitionType("test-v1")
                .eventTimestamp(Tools.nowUTC())
                .processingTimestamp(Tools.nowUTC())
                .id("event-1")
                .streams(ImmutableSet.of("stream-1"))
                .message("test")
                .source("source")
                .keyTuple(ImmutableList.of())
                .key("")
                .priority(2)
                .fields(ImmutableMap.of())
                .build();

        final EventDefinitionDto eventDefinitionDto = EventDefinitionDto.builder()
                .alert(true)
                .id("event-def-1")
                .title("Test Event Definition")
                .description("test")
                .config(new org.graylog.events.processor.EventProcessorConfig() {
                    @Override public String type() { return "test-v1"; }
                    @Override public org.graylog2.plugin.rest.ValidationResult validate(
                            org.graylog.security.UserContext userContext) { return null; }
                    @Override public org.graylog.events.contentpack.entities.EventProcessorConfigEntity toContentPackEntity(
                            org.graylog2.contentpacks.EntityDescriptorIds entityDescriptorIds) { return null; }
                })
                .fieldSpec(com.google.common.collect.ImmutableMap.of())
                .priority(2)
                .keySpec(ImmutableList.of())
                .notificationSettings(new org.graylog.events.notifications.EventNotificationSettings() {
                    @Override public long gracePeriodMs() { return 0; }
                    @Override public long backlogSize() { return 0; }
                    @Override public Builder toBuilder() { return null; }
                })
                .build();

        return EventNotificationContext.builder()
                .notificationId(notificationId)
                .notificationConfig(SlackEventNotificationConfig.builder()
                        .webhookUrl(WEBHOOK_URL)
                        .build())
                .event(eventDto)
                .eventDefinition(eventDefinitionDto)
                .build();
    }
}
