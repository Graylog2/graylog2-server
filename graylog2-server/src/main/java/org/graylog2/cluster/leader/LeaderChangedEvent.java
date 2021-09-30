package org.graylog2.cluster.leader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class LeaderChangedEvent {
    @JsonProperty("leader_node_id")
    public abstract String leaderNodeId();

    public static LeaderChangedEvent create(@JsonProperty("leader_node_id") String leaderNodeId) {
        return new AutoValue_LeaderChangedEvent(leaderNodeId);
    }
}
