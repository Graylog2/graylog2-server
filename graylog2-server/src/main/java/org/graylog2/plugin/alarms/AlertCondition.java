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
package org.graylog2.plugin.alarms;

import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

public interface AlertCondition {
    String getDescription();

    String getId();

    DateTime getCreatedAt();

    String getCreatorUserId();

    Stream getStream();

    Map<String, Object> getParameters();

    Integer getBacklog();

    int getGrace();

    String getTypeString();

    interface CheckResult {
        boolean isTriggered();
        String getResultDescription();
        AlertCondition getTriggeredCondition();
        DateTime getTriggeredAt();

        /**
         * The limited list of messages that matched the alert in the corresponding stream.
         *
         * @return list of message summaries
         */
        List<MessageSummary> getMatchingMessages();
    }
}
