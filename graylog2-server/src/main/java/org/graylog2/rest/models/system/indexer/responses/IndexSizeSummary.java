package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class IndexSizeSummary {
    @JsonProperty("events")
    public abstract long events();

    @JsonProperty("deleted")
    public abstract long deleted();

    @JsonProperty("bytes")
    public abstract long bytes();

    @JsonCreator
    public static IndexSizeSummary create(@JsonProperty("events") long events,
                                          @JsonProperty("deleted") long deleted,
                                          @JsonProperty("bytes") long bytes) {
        return new AutoValue_IndexSizeSummary(events, deleted, bytes);
    }
}
