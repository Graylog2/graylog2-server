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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.cluster.Node;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only wrapper around {@link SystemNotificationDto} that implements {@link Notification}.
 * Used by {@link NotificationServiceAdapter#all()} to satisfy the {@code List<Notification>} return type
 * expected by legacy callers.
 */
public class NotificationView implements Notification {

    private final SystemNotificationDto dto;

    public NotificationView(SystemNotificationDto dto) {
        this.dto = dto;
    }

    @Override
    public Type getType() {
        return Type.valueOf(dto.type().toUpperCase(Locale.ENGLISH));
    }

    @Nullable
    @Override
    public String getKey() {
        return dto.key();
    }

    @JsonProperty("priority")
    @Override
    public Severity getSeverity() {
        final String stored = dto.priority();
        final String normalized = "urgent".equals(stored) ? "HIGH" : stored.toUpperCase(Locale.ENGLISH);
        return Severity.valueOf(normalized);
    }

    @Override
    public String getNodeId() {
        return dto.nodeId();
    }

    @Override
    public DateTime getTimestamp() {
        return new DateTime(dto.triggeredAt().toEpochMilli(), DateTimeZone.UTC);
    }

    @Override
    public Object getDetail(String key) {
        final Map<String, Object> details = dto.details();
        return details != null ? details.get(key) : null;
    }

    @Override
    public Map<String, Object> getDetails() {
        return dto.details();
    }

    @Override
    public Notification addType(Type type) {
        throw new UnsupportedOperationException("NotificationView is read-only");
    }

    @Override
    public Notification addKey(String key) {
        throw new UnsupportedOperationException("NotificationView is read-only");
    }

    @Override
    public Notification addTimestamp(DateTime timestamp) {
        throw new UnsupportedOperationException("NotificationView is read-only");
    }

    @Override
    public Notification addSeverity(Severity severity) {
        throw new UnsupportedOperationException("NotificationView is read-only");
    }

    @Override
    public Notification addNode(Node node) {
        throw new UnsupportedOperationException("NotificationView is read-only");
    }

    @Override
    public Notification addNode(String nodeId) {
        throw new UnsupportedOperationException("NotificationView is read-only");
    }

    @Override
    public Notification addDetail(String key, Object value) {
        throw new UnsupportedOperationException("NotificationView is read-only");
    }
}
