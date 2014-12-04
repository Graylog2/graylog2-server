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
package org.graylog2.rest.resources.system.radio.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class RadioSummary {
    @JsonProperty
    public abstract String nodeId();

    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String transportAddress();

    @JsonProperty
    public abstract String lastSeen();

    @JsonProperty
    public abstract String shortNodeId();

    public static RadioSummary create(String nodeId,
                                      String type,
                                      String transportAddress,
                                      String lastSeen,
                                      String shortNodeId) {
        return new AutoValue_RadioSummary(nodeId, type, transportAddress, lastSeen, shortNodeId);
    }
}
