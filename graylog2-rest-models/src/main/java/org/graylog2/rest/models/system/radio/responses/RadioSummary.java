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
package org.graylog2.rest.models.system.radio.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static RadioSummary create(@JsonProperty("node_id") String nodeId,
                                      @JsonProperty("type") String type,
                                      @JsonProperty("transport_address") String transportAddress,
                                      @JsonProperty("last_seen") String lastSeen,
                                      @JsonProperty("short_node_id") String shortNodeId) {
        return new AutoValue_RadioSummary(nodeId, type, transportAddress, lastSeen, shortNodeId);
    }
}
