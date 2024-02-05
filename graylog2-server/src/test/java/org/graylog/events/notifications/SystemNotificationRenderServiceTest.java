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

import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.graylog.events.processor.systemnotification.SystemNotificationRenderService.TEMPLATE_BASE_PATH;
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
                .addTimestamp(DateTime.now(DateTimeZone.UTC));
        when(notificationService.getByTypeAndKey(any(), any())).thenReturn(Optional.of(notification));

        SystemNotificationRenderService.RenderResponse renderResponse =
                renderService.render(notification.getType(), null, SystemNotificationRenderService.Format.HTML, null);
        assertThat(renderResponse.title).isEqualToIgnoringWhitespace("Email Transport Configuration is missing or invalid!");
        assertThat(renderResponse.description).containsSequence("java.lang.Exception: My Test Exception");
        assertThat(renderResponse.description).containsSequence("<span>");
    }

    @Test
    void plainRenderTestWithReplace() {
        notification = new NotificationImpl()
                .addNode("node")
                .addSeverity(Notification.Severity.URGENT)
                .addType(Notification.Type.EMAIL_TRANSPORT_CONFIGURATION_INVALID)
                .addDetail("title", "title")
                .addDetail("description", "description")
                .addDetail("dummy", "dummy")
                .addDetail("exception", new Exception("My Test Exception"))
                .addTimestamp(DateTime.now(DateTimeZone.UTC));

        SystemNotificationRenderService.RenderResponse renderResponse = renderService.render(notification);
        assertThat(renderResponse.description).containsSequence("java.lang.Exception: My Test Exception");
        assertThat(renderResponse.description).doesNotContain("<span>");
    }

    @Test
    void testCloudTrue() {
        String url = "http://my.system.input";
        notification = new NotificationImpl()
                .addNode("node")
                .addSeverity(Notification.Severity.URGENT)
                .addType(Notification.Type.NO_INPUT_RUNNING)
                .addDetail("SYSTEM_INPUTS", url)
                .addDetail("exception", new Exception("My Test Exception"))
                .addTimestamp(DateTime.now(DateTimeZone.UTC));
        when(graylogConfig.isCloud()).thenReturn(true);

        SystemNotificationRenderService.RenderResponse renderResponse =
                renderService.render(notification, SystemNotificationRenderService.Format.HTML, null);
        assertThat(renderResponse.description).doesNotContain(url);
    }

    @Test
    void testCloudFalse() {
        String url = "http://my.system.input";
        notification = new NotificationImpl()
                .addNode("node")
                .addSeverity(Notification.Severity.URGENT)
                .addType(Notification.Type.NO_INPUT_RUNNING)
                .addDetail("SYSTEM_INPUTS", url)
                .addDetail("exception", new Exception("My Test Exception"))
                .addTimestamp(DateTime.now(DateTimeZone.UTC));
        when(graylogConfig.isCloud()).thenReturn(false);

        SystemNotificationRenderService.RenderResponse renderResponse =
                renderService.render(notification, SystemNotificationRenderService.Format.HTML, null);
        assertThat(renderResponse.description).containsSequence(url);
    }

    @Test
    void testFtlLooping() {
        List<String[]> blockDetails = new ArrayList<>();
        blockDetails.add(new String[]{"11", "12"});
        blockDetails.add(new String[]{"21", "22"});
        notification = new NotificationImpl()
                .addNode("node")
                .addSeverity(Notification.Severity.URGENT)
                .addType(Notification.Type.ES_INDEX_BLOCKED)
                .addDetail("title", "my title")
                .addDetail("description", "my description")
                .addDetail("blockDetails", blockDetails)
                .addTimestamp(DateTime.now(DateTimeZone.UTC));
        when(notificationService.getByTypeAndKey(any(), any())).thenReturn(Optional.of(notification));

        SystemNotificationRenderService.RenderResponse renderResponse =
                renderService.render(notification, SystemNotificationRenderService.Format.HTML, null);
        assertThat(renderResponse.description).containsSequence("11: 12");
    }

    @Test
    void missingTemplates() {
        for (SystemNotificationRenderService.Format f : SystemNotificationRenderService.Format.values()) {
            String basePath = TEMPLATE_BASE_PATH + f.toString() + "/";
            Arrays.stream(Notification.Type.values())
                    // deprecated notification types from pre-5.0.
                    .filter(t -> t != Notification.Type.MULTI_MASTER && t != Notification.Type.NO_MASTER)
                    .forEach(t -> {
                        String templateFile = basePath + t.toString().toLowerCase(Locale.ENGLISH) + ".ftl";
                        if (SystemNotificationRenderServiceTest.class.getResource(templateFile) == null) {
                            fail("Missing template: " + templateFile);
                        }
                    });
        }
    }
}
