package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class IndexRangeSummary {
    @JsonProperty("index_name")
    public abstract String indexName();

    @JsonProperty("calculated_at")
    public abstract DateTime calculatedAt();

    @JsonProperty("start")
    public abstract DateTime start();

    @JsonProperty("calculation_took_ms")
    public abstract int calculationTookMs();

    @JsonCreator
    public static IndexRangeSummary create(@JsonProperty("index_name") String indexName,
                                           @JsonProperty("calculated_at") DateTime calculatedAt,
                                           @JsonProperty("start") DateTime start,
                                           @JsonProperty("calculation_took_ms") int calculationTookMs) {
        return new AutoValue_IndexRangeSummary(indexName, calculatedAt, start, calculationTookMs);
    }
}
