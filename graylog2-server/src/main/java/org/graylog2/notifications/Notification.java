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
import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTime;

public interface Notification extends Persisted {
    Notification addType(Type type);

    Notification addTimestamp(DateTime timestamp);

    Notification addSeverity(Severity severity);

    Notification addNode(Node node);

    DateTime getTimestamp();

    Type getType();

    Severity getSeverity();

    String getNodeId();

    Notification addDetail(String key, Object value);

    Object getDetail(String key);

    Notification addNode(String nodeId);

    enum Type {
        DEFLECTOR_EXISTS_AS_INDEX,
        MULTI_MASTER,
        NO_MASTER,
        ES_OPEN_FILES,
        ES_CLUSTER_RED,
        ES_UNAVAILABLE,
        NO_INPUT_RUNNING,
        INPUT_FAILED_TO_START,
        INPUT_FAILURE_SHUTDOWN,
        CHECK_SERVER_CLOCKS,
        OUTDATED_VERSION,
        EMAIL_TRANSPORT_CONFIGURATION_INVALID,
        EMAIL_TRANSPORT_FAILED,
        STREAM_PROCESSING_DISABLED,
        GC_TOO_LONG,
        JOURNAL_UTILIZATION_TOO_HIGH,
        JOURNAL_UNCOMMITTED_MESSAGES_DELETED,
        OUTPUT_DISABLED,
        OUTPUT_FAILING,
        INDEX_RANGES_RECALCULATION,
        GENERIC,
        ES_NODE_DISK_WATERMARK_LOW,
        ES_NODE_DISK_WATERMARK_HIGH,
        ES_NODE_DISK_WATERMARK_FLOOD_STAGE,
        ES_VERSION_MISMATCH,
        LEGACY_LDAP_CONFIG_MIGRATION
    }

    enum Severity {
        NORMAL, URGENT
    }
}
