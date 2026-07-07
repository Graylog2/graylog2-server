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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.NotificationTestData;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog.events.processor.EventDefinitionDto;

@Singleton
public class UrlAllowlistValidator {
    private final UrlAllowlistService allowlistService;
    private final UrlAllowlistNotificationService allowlistNotificationService;

    @Inject
    public UrlAllowlistValidator(UrlAllowlistService allowlistService,
                                 UrlAllowlistNotificationService allowlistNotificationService) {
        this.allowlistService = allowlistService;
        this.allowlistNotificationService = allowlistNotificationService;
    }

    public void validateUrl(String url, EventNotificationContext ctx) throws TemporaryEventNotificationException {
        if (!allowlistService.isAllowlisted(url)) {
            if (!NotificationTestData.TEST_NOTIFICATION_ID.equals(ctx.notificationId())) {
                final String eventDefTitle = ctx.eventDefinition().map(EventDefinitionDto::title).orElse("Unnamed");
                final String description = "The alert notification \"" + eventDefTitle +
                        "\" is trying to access a URL which is not allowlisted. Please check your configuration. [url: \"" +
                        url + "\"]";
                allowlistNotificationService.publishAllowlistFailure(description);
            }
            throw new TemporaryEventNotificationException("URL <" + url + "> is not allowlisted.");
        }
    }
}
