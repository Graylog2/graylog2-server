package org.graylog2.rest.models.system.cluster.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NodeSummary {
    @JsonProperty
    public abstract String nodeId();
    @JsonProperty
    public abstract String type();
    @JsonProperty("is_master")
    public abstract boolean isMaster();
    @JsonProperty
    public abstract String transportAddress();
    @JsonProperty
    public abstract String lastSeen();
    @JsonProperty
    public abstract String shortNodeId();

    @JsonCreator
    public static NodeSummary create(@JsonProperty("node_id") String nodeId,
                                     @JsonProperty("type") String type,
                                     @JsonProperty("is_master") boolean isMaster,
                                     @JsonProperty("transport_address") String transportAddress,
                                     @JsonProperty("last_seen") String lastSeen,
                                     @JsonProperty("short_node_id") String shortNodeId) {
        return new AutoValue_NodeSummary(nodeId, type, isMaster, transportAddress, lastSeen, shortNodeId);
    }
}
