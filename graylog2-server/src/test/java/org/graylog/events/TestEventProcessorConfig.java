/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventProcessorConfigEntity;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorSchedulerConfig;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.rest.ValidationResult;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@AutoValue
@JsonDeserialize(builder = TestEventProcessorConfig.Builder.class)
public abstract class TestEventProcessorConfig implements EventProcessorConfig {
    public static final String TYPE_NAME = "__test_event_processor_config__";

    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_SEARCH_WITHIN_MS = "search_within_ms";
    private static final String FIELD_EXECUTE_EVERY_MS = "execute_every_ms";

    @JsonProperty(FIELD_MESSAGE)
    public abstract String message();

    @JsonProperty(FIELD_SEARCH_WITHIN_MS)
    public abstract long searchWithinMs();

    @JsonProperty(FIELD_EXECUTE_EVERY_MS)
    public abstract long executeEveryMs();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @Override
    public Optional<EventProcessorSchedulerConfig> toJobSchedulerConfig(EventDefinition eventDefinition, JobSchedulerClock clock) {
        final DateTime now = clock.nowUTC();

        // We need an initial timerange for the first execution of the event processor
        final AbsoluteRange timerange = AbsoluteRange.create(now.minus(searchWithinMs()), now);

        final EventProcessorExecutionJob.Config jobDefinitionConfig = EventProcessorExecutionJob.Config.builder()
                .eventDefinitionId(eventDefinition.id())
                .processingWindowSize(searchWithinMs())
                .processingHopSize(executeEveryMs())
                .parameters(TestEventProcessorParameters.builder()
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
            return new AutoValue_TestEventProcessorConfig.Builder().type(TYPE_NAME);
        }

        @JsonProperty(FIELD_MESSAGE)
        public abstract Builder message(String message);

        @JsonProperty(FIELD_SEARCH_WITHIN_MS)
        public abstract Builder searchWithinMs(long searchWithinMs);

        @JsonProperty(FIELD_EXECUTE_EVERY_MS)
        public abstract Builder executeEveryMs(long executeEveryMs);

        public abstract TestEventProcessorConfig build();
    }

    @Override
    public EventProcessorConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return null;
    }

    @Override
    public ValidationResult validate() {
        return new ValidationResult();
    }
}
