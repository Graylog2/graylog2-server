package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = AggregationResult.Builder.class)
public abstract class AggregationResult {
    public abstract ImmutableList<AggregationKeyResult> keyResults();

    public abstract AbsoluteRange effectiveTimerange();

    public abstract long totalAggregatedMessages();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationResult.Builder();
        }

        public abstract Builder keyResults(List<AggregationKeyResult> keyResults);

        public abstract Builder effectiveTimerange(AbsoluteRange effectiveTimerange);

        public abstract Builder totalAggregatedMessages(long total);

        public abstract AggregationResult build();
    }
}