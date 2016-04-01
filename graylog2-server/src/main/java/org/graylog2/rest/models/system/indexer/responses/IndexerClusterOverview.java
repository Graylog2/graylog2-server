package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class IndexerClusterOverview {
    @JsonProperty("health")
    public abstract ClusterHealth health();

    @JsonProperty("name")
    public abstract String name();

    @JsonCreator
    public static IndexerClusterOverview create(@JsonProperty("health") ClusterHealth health,
                                                @JsonProperty("name") String name) {
        return new AutoValue_IndexerClusterOverview(health, name);
    }
}
