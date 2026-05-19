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

import org.bson.Document;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationBuilder;
import org.graylog2.notifications.SystemNotificationDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class V20260430000000_MigrateNotificationsToSystemNotifications extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20260430000000_MigrateNotificationsToSystemNotifications.class);
    private static final String OLD_COLLECTION = "notifications";

    private final ClusterConfigService clusterConfigService;
    private final MongoCollections mongoCollections;
    private final SystemNotificationRenderService renderService;

    @Inject
    public V20260430000000_MigrateNotificationsToSystemNotifications(
            ClusterConfigService clusterConfigService,
            MongoCollections mongoCollections,
            SystemNotificationRenderService renderService) {
        this.clusterConfigService = clusterConfigService;
        this.mongoCollections = mongoCollections;
        this.renderService = renderService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-04-30T00:00:00Z");
    }

    @Override
    public void upgrade() {
        if (Objects.nonNull(clusterConfigService.get(MigrationCompleted.class))) {
            LOG.debug("Migration already completed, skipping.");
            return;
        }

        final com.mongodb.client.MongoCollection<Document> oldCollection = mongoCollections.nonEntityCollection(OLD_COLLECTION, Document.class);
        final MongoCollection<SystemNotificationDto> newCollection =
                mongoCollections.collection("system_notifications", SystemNotificationDto.class);

        int migrated = 0;
        int errors = 0;

        for (final Document doc : oldCollection.find()) {
            try {
                final String typeStr = doc.getString("type");
                final String key = doc.getString("key");
                final String severity = doc.getString("severity");
                final String nodeId = doc.getString("node_id");

                @SuppressWarnings("unchecked")
                final Map<String, Object> details = doc.get("details", Map.class);

                // Convert timestamp
                final Instant triggeredAt;
                final Object tsObj = doc.get("timestamp");
                if (tsObj instanceof Date date) {
                    triggeredAt = date.toInstant();
                } else if (tsObj instanceof String tsStr) {
                    triggeredAt = Instant.parse(tsStr);
                } else {
                    triggeredAt = Instant.now();
                }

                // Render title and description
                String title = null;
                String description = null;
                try {
                    final Notification.Type notifType = Notification.Type.valueOf(typeStr.toUpperCase(Locale.ENGLISH));
                    final Notification tempNotification = new NotificationBuilder()
                            .addType(notifType)
                            .addNode(nodeId != null ? nodeId : "");
                    if (details != null) {
                        details.forEach(tempNotification::addDetail);
                    }
                    final var rendered = renderService.render(
                            tempNotification,
                            SystemNotificationRenderService.Format.PLAINTEXT,
                            null
                    );
                    title = rendered.title != null ? rendered.title.strip() : null;
                    description = rendered.description != null ? rendered.description.strip() : null;
                } catch (Exception e) {
                    LOG.warn("Could not render notification of type {}: {}", typeStr, e.getMessage());
                }

                final SystemNotificationDto dto = SystemNotificationDto.builder()
                        .type(typeStr)
                        .key(key)
                        .severity(severity != null ? severity : "normal")
                        .nodeId(nodeId != null ? nodeId : "")
                        .title(title)
                        .description(description)
                        .details(details != null ? details : Map.of())
                        .isRead(false)
                        .actor(null)
                        .lastChanged(null)
                        .triggeredAt(triggeredAt)
                        .build();

                newCollection.insertOne(dto);
                migrated++;
            } catch (Exception e) {
                LOG.error("Failed to migrate notification document {}: {}", doc.get("_id"), e.getMessage(), e);
                errors++;
            }
        }

        LOG.info("Migrated {} notifications to system_notifications ({} errors)", migrated, errors);

        // Drop the old collection
        oldCollection.drop();
        LOG.info("Dropped old '{}' collection", OLD_COLLECTION);

        clusterConfigService.write(new MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
