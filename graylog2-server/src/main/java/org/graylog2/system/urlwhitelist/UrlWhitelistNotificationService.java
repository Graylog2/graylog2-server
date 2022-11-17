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
package org.graylog2.system.urlwhitelist;

import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UrlWhitelistNotificationService {

    private final NotificationService notificationService;

    @Inject
    public UrlWhitelistNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Publish a system notification indicating that there was an attempt to access a URL which is not whitelisted.
     *
     * <p>This method is synchronized to reduce the chance of emitting multiple notifications at the same time</p>
     *
     * @param description The description of the notification.
     */
    synchronized public void publishWhitelistFailure(String description) {
        final Notification notification = notificationService.buildNow()
                .addType(Notification.Type.GENERIC)
                .addSeverity(Notification.Severity.NORMAL)
                .addDetail("title", "URL not whitelisted.")
                .addDetail("description", description);
        notificationService.publishIfFirst(notification);
    }

}
