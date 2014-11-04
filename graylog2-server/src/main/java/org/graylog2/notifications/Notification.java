/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.notifications;

import org.graylog2.cluster.Node;
import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
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

    Map<String, Object> asMap();

    Notification addNode(String nodeId);

    public enum Type {
        DEFLECTOR_EXISTS_AS_INDEX,
        MULTI_MASTER,
        NO_MASTER,
        ES_OPEN_FILES,
        NO_INPUT_RUNNING,
        INPUT_FAILED_TO_START,
        CHECK_SERVER_CLOCKS,
        OUTDATED_VERSION,
        EMAIL_TRANSPORT_CONFIGURATION_INVALID,
        EMAIL_TRANSPORT_FAILED,
        STREAM_PROCESSING_DISABLED,
        GC_TOO_LONG
    }

    public enum Severity {
        NORMAL, URGENT
    }
}
