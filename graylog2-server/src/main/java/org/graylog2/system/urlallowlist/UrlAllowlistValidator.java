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
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.processor.EventDefinitionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Validates webhook URLs against the URL allowlist for Slack and Teams notifications.
// By default, non-allowlisted URLs log a warning. When UrlAllowlist.enforceForNotifications()
// is enabled, non-allowlisted URLs throw PermanentEventNotificationException instead.
// Called from SlackEventNotification, TeamsEventNotification, and TeamsEventNotificationV2.
// To remove: delete this class, the enforceForNotifications field on UrlAllowlist, the
// validateUrl calls in the three notification classes, and the frontend checkbox in UrlAllowListForm.
@Singleton
public class UrlAllowlistValidator {
    private static final Logger LOG = LoggerFactory.getLogger(UrlAllowlistValidator.class);

    private final UrlAllowlistService allowlistService;
    private final UrlAllowlistNotificationService allowlistNotificationService;

    @Inject
    public UrlAllowlistValidator(UrlAllowlistService allowlistService,
                                 UrlAllowlistNotificationService allowlistNotificationService) {
        this.allowlistService = allowlistService;
        this.allowlistNotificationService = allowlistNotificationService;
    }

    public void validateUrl(String url, EventNotificationContext ctx) throws PermanentEventNotificationException {
        if (!allowlistService.isAllowlisted(url)) {
            if (!NotificationTestData.TEST_NOTIFICATION_ID.equals(ctx.notificationId())) {
                final String eventDefTitle = ctx.eventDefinition().map(EventDefinitionDto::title).orElse("Unnamed");
                final String description = "The alert notification " + eventDefTitle +
                        " is trying to access a URL which is not allowlisted. Please add it to the URL allowlist. " +
                        "[url: " + url + "]";
                allowlistNotificationService.publishAllowlistFailure(description);
            }
            if (allowlistService.getAllowlist().enforceForNotifications()) {
                LOG.warn("Blocking notification because webhook URL is not allowlisted. " +
                        "[url: {}, notification: {}]", url, ctx.notificationId());
                throw new PermanentEventNotificationException("URL <" + url + "> is not allowlisted.");
            }
            LOG.warn("Notification is using a URL which is not allowlisted. " +
                    "Enable \"Enforce for Slack & Teams notifications\" in System > Configurations > URL Allowlist to block non-allowlisted URLs. " +
                    "[url: {}, notification: {}]", url, ctx.notificationId());
        }
    }
}
