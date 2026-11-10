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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;

import java.time.ZonedDateTime;

/**
 * Removes stored {@code ES_SHARD_ALLOCATION_MAXIMUM} notifications. The check that published them compared shard
 * counts per node, while OpenSearch applies {@code cluster.max_shards_per_node} to the whole cluster, so they may
 * report a non-existent {@code UNASSIGNED} node. The notification is no longer published and nothing would ever
 * resolve the stored ones.
 */
public class V20261029120000_RemoveShardAllocationMaximumNotifications extends Migration {
    private final NotificationService notificationService;

    @Inject
    public V20261029120000_RemoveShardAllocationMaximumNotifications(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-10-29T12:00:00Z");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void upgrade() {
        // Idempotent: removing notifications of a type that is no longer published needs no completion marker.
        notificationService.fixed(Notification.Type.ES_SHARD_ALLOCATION_MAXIMUM);
    }
}
