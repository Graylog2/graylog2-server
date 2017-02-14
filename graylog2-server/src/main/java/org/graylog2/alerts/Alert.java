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
package org.graylog2.alerts;

import org.joda.time.DateTime;

import java.util.Map;

public interface Alert {
    String getId();
    String getStreamId();
    String getConditionId();
    DateTime getTriggeredAt();
    DateTime getResolvedAt();
    String getDescription();
    Map<String, Object> getConditionParameters();
    boolean isInterval();

    enum AlertState {
        ANY, RESOLVED, UNRESOLVED;

        public static AlertState fromString(String state) {
            for (AlertState aState : AlertState.values()) {
                if (aState.toString().equalsIgnoreCase(state)) {
                    return aState;
                }
            }

            throw new IllegalArgumentException("Alert state " + state + " is not supported");
        }
    }
}
