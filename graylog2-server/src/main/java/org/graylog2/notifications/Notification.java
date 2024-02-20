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

import javax.annotation.Nullable;
import java.util.Map;

public interface Notification extends Persisted {
    // Some pre-defined detail keys
    final String KEY_TITLE = "title";
    final String KEY_DESCRIPTION = "description";

    Notification addType(Type type);

    Notification addKey(String key);

    Notification addTimestamp(DateTime timestamp);

    Notification addSeverity(Severity severity);

    Notification addNode(Node node);

    DateTime getTimestamp();

    Type getType();

    @Nullable
    String getKey();

    Severity getSeverity();

    String getNodeId();

    Notification addDetail(String key, Object value);

    Object getDetail(String key);

    Map<String, Object> getDetails();

    Notification addNode(String nodeId);

    enum Type {
        DATA_NODE_NEEDS_PROVISIONING,
        DEFLECTOR_EXISTS_AS_INDEX,
        @Deprecated MULTI_MASTER, // use MULTI_LEADER instead
        @Deprecated NO_MASTER, // use NO_LEADER instead
        ES_OPEN_FILES,
        ES_CLUSTER_RED,
        ES_UNAVAILABLE,
        NO_INPUT_RUNNING,
        INPUT_FAILED_TO_START,
        INPUT_FAILING,
        INPUT_FAILURE_SHUTDOWN,
        CHECK_SERVER_CLOCKS,
        OUTDATED_VERSION,
        EMAIL_TRANSPORT_CONFIGURATION_INVALID,
        EMAIL_TRANSPORT_FAILED,
        STREAM_PROCESSING_DISABLED,
        @Deprecated GC_TOO_LONG,
        JOURNAL_UTILIZATION_TOO_HIGH,
        JOURNAL_UNCOMMITTED_MESSAGES_DELETED,
        OUTPUT_DISABLED,
        OUTPUT_FAILING,
        INDEX_RANGES_RECALCULATION,
        GENERIC,
        GENERIC_WITH_LINK,
        ES_INDEX_BLOCKED,
        ES_NODE_DISK_WATERMARK_LOW,
        ES_NODE_DISK_WATERMARK_HIGH,
        ES_NODE_DISK_WATERMARK_FLOOD_STAGE,
        ES_VERSION_MISMATCH,
        LEGACY_LDAP_CONFIG_MIGRATION,
        MULTI_LEADER,
        NO_LEADER,
        ARCHIVING_SUMMARY,
        SEARCH_ERROR,
        SIDECAR_STATUS_UNKNOWN,
        CERTIFICATE_NEEDS_RENEWAL,
        EVENT_LIMIT_REACHED
    }

    enum Severity {
        NORMAL, URGENT
    }
}
