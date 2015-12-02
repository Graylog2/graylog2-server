package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class BulkIndices {
    @JsonProperty("closed")
    public abstract ClosedIndices closed();

    @JsonProperty("reopened")
    public abstract ClosedIndices reopened();

    @JsonProperty("all")
    public abstract AllIndicesInfo all();

    @JsonCreator
    public static BulkIndices create(@JsonProperty("closed") ClosedIndices closed,
                                         @JsonProperty("reopened") ClosedIndices reopened,
                                         @JsonProperty("all") AllIndicesInfo all) {
        return new AutoValue_BulkIndices(closed, reopened, all);
    }
}
