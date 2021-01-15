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
package org.graylog.events.processor.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.processor.EventProcessorParametersWithTimerange;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@AutoValue
@JsonTypeName(AggregationEventProcessorConfig.TYPE_NAME)
@JsonDeserialize(builder = AggregationEventProcessorParameters.Builder.class)
public abstract class AggregationEventProcessorParameters implements EventProcessorParametersWithTimerange {
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_BATCH_SIZE = "batch_size";

    @JsonProperty(FIELD_STREAMS)
    public abstract ImmutableSet<String> streams();

    @JsonProperty(FIELD_BATCH_SIZE)
    public abstract int batchSize();

    @Override
    public EventProcessorParametersWithTimerange withTimerange(DateTime from, DateTime to) {
        requireNonNull(from, "from cannot be null");
        requireNonNull(to, "to cannot be null");
        checkArgument(to.isAfter(from), "to must be after from");

        return toBuilder().timerange(AbsoluteRange.create(from, to)).build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventProcessorParametersWithTimerange.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            final RelativeRange timerange;
            try {
                timerange = RelativeRange.create(3600);
            } catch (InvalidRangeParametersException e) {
                // This should not happen!
                throw new RuntimeException(e);
            }

            return new AutoValue_AggregationEventProcessorParameters.Builder()
                    .type(AggregationEventProcessorConfig.TYPE_NAME)
                    .timerange(timerange)
                    .streams(Collections.emptySet())
                    .batchSize(500);
        }

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_BATCH_SIZE)
        public abstract Builder batchSize(int batchSize);

        public abstract AggregationEventProcessorParameters build();
    }
}
