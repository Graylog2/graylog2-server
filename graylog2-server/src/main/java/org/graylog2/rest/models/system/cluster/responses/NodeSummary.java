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
package org.graylog2.rest.models.system.cluster.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class NodeSummary {
    @JsonProperty
    public abstract String clusterId();
    @JsonProperty
    public abstract String nodeId();
    @JsonProperty
    public abstract String type();
    @JsonProperty("is_primary")
    public abstract boolean isPrimary();
    @JsonProperty
    public abstract String transportAddress();
    @JsonProperty
    public abstract String lastSeen();
    @JsonProperty
    public abstract String shortNodeId();
    @JsonProperty
    public abstract String hostname();

    @JsonCreator
    public static NodeSummary create(@JsonProperty("cluster_id") String clusterId,
                                     @JsonProperty("node_id") String nodeId,
                                     @JsonProperty("type") String type,
                                     @JsonProperty("is_primary") boolean isPrimary,
                                     @JsonProperty("transport_address") String transportAddress,
                                     @JsonProperty("last_seen") String lastSeen,
                                     @JsonProperty("short_node_id") String shortNodeId,
                                     @JsonProperty("hostname") String hostname) {
        return new AutoValue_NodeSummary(clusterId, nodeId, type, isPrimary, transportAddress, lastSeen, shortNodeId, hostname);
    }
}
