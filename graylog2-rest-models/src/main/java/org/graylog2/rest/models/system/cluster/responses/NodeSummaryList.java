package org.graylog2.rest.models.system.cluster.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class NodeSummaryList {
    @JsonProperty
    public abstract List<NodeSummary> nodes();
    @JsonProperty
    public abstract int total();

    @JsonCreator
    public static NodeSummaryList create(@JsonProperty("nodes") List<NodeSummary> nodes,
                                         @JsonProperty("total") int total) {
        return new AutoValue_NodeSummaryList(nodes, total);
    }

    public static NodeSummaryList create(List<NodeSummary> nodes) {
        return new AutoValue_NodeSummaryList(nodes, nodes.size());
    }
}
