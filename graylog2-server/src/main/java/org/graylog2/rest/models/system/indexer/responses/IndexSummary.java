package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class IndexSummary {
    @JsonProperty("size")
    @Nullable
    public abstract IndexSizeSummary size();

    @JsonProperty("range")
    @Nullable
    public abstract IndexRangeSummary range();

    @JsonProperty("is_deflector")
    public abstract boolean isDeflector();

    @JsonProperty("is_closed")
    public abstract boolean isClosed();

    @JsonProperty("is_reopened")
    public abstract boolean isReopened();

    @JsonCreator
    public static IndexSummary create(@JsonProperty("size") @Nullable IndexSizeSummary size,
                                      @JsonProperty("range") @Nullable IndexRangeSummary range,
                                      @JsonProperty("is_deflector") boolean isDeflector,
                                      @JsonProperty("is_closed") boolean isClosed,
                                      @JsonProperty("is_reopened") boolean isReopened) {
        return new AutoValue_IndexSummary(size, range, isDeflector, isClosed, isReopened);
    }
}
