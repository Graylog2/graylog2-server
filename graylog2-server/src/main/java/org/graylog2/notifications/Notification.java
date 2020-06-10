/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
        MULTI_PRIMARY,
        NO_PRIMARY,
        ES_OPEN_FILES,
        ES_CLUSTER_RED,
        ES_UNAVAILABLE,
        NO_INPUT_RUNNING,
        INPUT_FAILED_TO_START,
        CHECK_SERVER_CLOCKS,
        OUTDATED_VERSION,
        EMAIL_TRANSPORT_CONFIGURATION_INVALID,
        EMAIL_TRANSPORT_FAILED,
        STREAM_PROCESSING_DISABLED,
        GC_TOO_LONG,
        JOURNAL_UTILIZATION_TOO_HIGH,
        JOURNAL_UNCOMMITTED_MESSAGES_DELETED,
        OUTPUT_DISABLED,
        INDEX_RANGES_RECALCULATION,
        GENERIC,
        ES_NODE_DISK_WATERMARK_LOW,
        ES_NODE_DISK_WATERMARK_HIGH,
        ES_NODE_DISK_WATERMARK_FLOOD_STAGE
    }

    enum Severity {
        NORMAL, URGENT
    }
}
