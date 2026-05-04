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
package org.graylog2.notifications;

import org.graylog2.cluster.Node;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter implementing the legacy {@link NotificationService} interface, delegating all operations
 * to {@link SystemNotificationService}. This allows ~42 existing callers across graylog2-server
 * and graylog-plugin-enterprise to work transparently without code changes.
 */
@Singleton
public class NotificationServiceAdapter implements NotificationService {

    private final SystemNotificationService systemNotificationService;
    private final NodeId nodeId;
    private final NotificationSystemEventPublisher eventPublisher;

    @Inject
    public NotificationServiceAdapter(SystemNotificationService systemNotificationService,
                                      NodeId nodeId,
                                      NotificationSystemEventPublisher eventPublisher) {
        this.systemNotificationService = systemNotificationService;
        this.nodeId = nodeId;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Notification build() {
        return new NotificationBuilder();
    }

    @Override
    public Notification buildNow() {
        final Notification notification = build();
        notification.addTimestamp(Tools.nowUTC());
        return notification;
    }

    @Override
    public boolean fixed(Notification.Type type) {
        return systemNotificationService.markAsRead(type, SystemNotificationDto.Actor.system());
    }

    @Override
    public boolean fixed(Notification.Type type, String key) {
        return systemNotificationService.markAsRead(type, key, SystemNotificationDto.Actor.system());
    }

    @Override
    public boolean fixed(Notification.Type type, Node node) {
        // node parameter is ignored -- no external callers use this overload,
        // and node_id was never part of the deduplication logic.
        return systemNotificationService.markAsRead(type, SystemNotificationDto.Actor.system());
    }

    @Override
    public boolean fixed(Notification notification) {
        // Matches current NotificationServiceImpl behavior which delegates to fixed(type, (Node) null)
        return systemNotificationService.markAsRead(notification.getType(), SystemNotificationDto.Actor.system());
    }

    @Override
    public boolean isFirst(Notification.Type type) {
        return !systemNotificationService.hasUnread(type);
    }

    @Override
    public List<Notification> all() {
        return systemNotificationService.findAllUnread().stream()
                .map(dto -> (Notification) new NotificationView(dto))
                .toList();
    }

    @Override
    public Optional<Notification> getByTypeAndKey(Notification.Type type, @Nullable String key) {
        return systemNotificationService.findByTypeAndKey(type, key)
                .map(dto -> (Notification) new NotificationView(dto));
    }

    @Override
    public boolean publishIfFirst(Notification notification) {
        // Ensure node ID is set
        if (notification.getNodeId() == null) {
            notification.addNode(nodeId.getNodeId());
        }

        // Ensure timestamp is set
        if (notification.getTimestamp() == null) {
            notification.addTimestamp(Tools.nowUTC());
        }

        final boolean inserted = systemNotificationService.publish(
                notification.getType(),
                notification.getKey(),
                notification.getSeverity(),
                notification.getNodeId(),
                notification.getDetails()
        );

        if (inserted) {
            eventPublisher.submit(notification);
        }

        return inserted;
    }

    @Override
    public int destroyAllByType(Notification.Type type) {
        systemNotificationService.markAsRead(type, SystemNotificationDto.Actor.system());
        return 1;
    }

    @Override
    public int destroyAllByTypeAndKey(Notification.Type type, @Nullable String key) {
        if (key != null) {
            systemNotificationService.markAsRead(type, key, SystemNotificationDto.Actor.system());
        } else {
            systemNotificationService.markAsRead(type, SystemNotificationDto.Actor.system());
        }
        return 1;
    }
}
