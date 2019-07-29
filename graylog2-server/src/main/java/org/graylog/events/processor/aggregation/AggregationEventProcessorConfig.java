package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorSchedulerConfig;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.rest.ValidationResult;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@AutoValue
@JsonTypeName(AggregationEventProcessorConfig.TYPE_NAME)
@JsonDeserialize(builder = AggregationEventProcessorConfig.Builder.class)
public abstract class AggregationEventProcessorConfig implements EventProcessorConfig {
    public static final String TYPE_NAME = "aggregation-v1";

    private static final String FIELD_QUERY = "query";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_GROUP_BY = "group_by";
    private static final String FIELD_SERIES = "series";
    private static final String FIELD_CONDITIONS = "conditions";
    private static final String FIELD_SEARCH_WITHIN_MS = "search_within_ms";
    private static final String FIELD_EXECUTE_EVERY_MS = "execute_every_ms";

    @JsonProperty(FIELD_QUERY)
    public abstract String query();

    @JsonProperty(FIELD_STREAMS)
    public abstract ImmutableSet<String> streams();

    @JsonProperty(FIELD_GROUP_BY)
    public abstract List<String> groupBy();

    @JsonProperty(FIELD_SERIES)
    public abstract List<AggregationSeries> series();

    @JsonProperty(FIELD_CONDITIONS)
    public abstract Optional<AggregationConditions> conditions();

    @JsonProperty(FIELD_SEARCH_WITHIN_MS)
    public abstract long searchWithinMs();

    @JsonProperty(FIELD_EXECUTE_EVERY_MS)
    public abstract long executeEveryMs();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public Optional<EventProcessorSchedulerConfig> toJobSchedulerConfig(EventDefinition eventDefinition, JobSchedulerClock clock) {
        final DateTime now = clock.nowUTC();

        // We need an initial timerange for the first execution of the event processor
        final AbsoluteRange timerange = AbsoluteRange.create(now.minus(searchWithinMs()), now);

        final EventProcessorExecutionJob.Config jobDefinitionConfig = EventProcessorExecutionJob.Config.builder()
                .eventDefinitionId(eventDefinition.id())
                .processingWindowSize(searchWithinMs())
                .processingHopSize(executeEveryMs())
                .parameters(AggregationEventProcessorParameters.builder()
                        .timerange(timerange)
                        .build())
                .build();
        final IntervalJobSchedule schedule = IntervalJobSchedule.builder()
                .interval(executeEveryMs())
                .unit(TimeUnit.MILLISECONDS)
                .build();

        return Optional.of(EventProcessorSchedulerConfig.create(jobDefinitionConfig, schedule));
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventProcessorConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AggregationEventProcessorConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(String query);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_GROUP_BY)
        public abstract Builder groupBy(List<String> groupBy);

        @JsonProperty(FIELD_SERIES)
        public abstract Builder series(List<AggregationSeries> series);

        @JsonProperty(FIELD_CONDITIONS)
        public abstract Builder conditions(@Nullable AggregationConditions conditions);

        @JsonProperty(FIELD_SEARCH_WITHIN_MS)
        public abstract Builder searchWithinMs(long searchWithinMs);

        @JsonProperty(FIELD_EXECUTE_EVERY_MS)
        public abstract Builder executeEveryMs(long executeEveryMs);

        public abstract AggregationEventProcessorConfig build();
    }

    @Override
    public ValidationResult validate() {
        return new ValidationResult();
    }
}
