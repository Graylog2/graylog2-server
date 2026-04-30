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
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable builder implementing {@link Notification}, used by
 * {@link NotificationService#build()} and {@link NotificationService#buildNow()}.
 * Replaces the legacy {@code NotificationImpl} for constructing notifications
 * before passing them to {@link NotificationService#publishIfFirst(Notification)}.
 */
public class NotificationBuilder implements Notification {

    private Type type;
    @Nullable
    private String key;
    private Severity severity = Severity.NORMAL;
    @Nullable
    private DateTime timestamp;
    @Nullable
    private String nodeId;
    private final Map<String, Object> details = new HashMap<>();

    @Override
    public Notification addType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public Notification addKey(String key) {
        this.key = key;
        return this;
    }

    @Override
    public Notification addTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public Notification addSeverity(Severity severity) {
        this.severity = severity;
        return this;
    }

    @Override
    public Notification addNode(Node node) {
        this.nodeId = node.getNodeId();
        return this;
    }

    @Override
    public Notification addNode(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    @Override
    public Notification addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Nullable
    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public Object getDetail(String key) {
        return details.get(key);
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }
}
