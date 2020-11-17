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
package org.graylog2.rest.models.system.processing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.system.processing.ProcessingStatusDto;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = ProcessingStatusSummary.Builder.class)
public abstract class ProcessingStatusSummary {
    public static final String FIELD_RECEIVE_TIMES = "receive_times";

    @JsonProperty(FIELD_RECEIVE_TIMES)
    public abstract ReceiveTimes receiveTimes();

    public static Builder builder() {
        return Builder.create();
    }

    public static ProcessingStatusSummary of(ProcessingStatusRecorder processingStatusRecorder) {
        return builder()
                .receiveTimes(ReceiveTimes.builder()
                        .ingest(processingStatusRecorder.getIngestReceiveTime())
                        .postProcessing(processingStatusRecorder.getPostProcessingReceiveTime())
                        .postIndexing(processingStatusRecorder.getPostIndexingReceiveTime())
                        .build())
                .build();
    }

    public static ProcessingStatusSummary of(ProcessingStatusDto dto) {
        return builder()
                .receiveTimes(ReceiveTimes.builder()
                        .ingest(dto.receiveTimes().ingest())
                        .postProcessing(dto.receiveTimes().postProcessing())
                        .postIndexing(dto.receiveTimes().postIndexing())
                        .build())
                .build();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ProcessingStatusSummary.Builder();
        }

        @JsonProperty(FIELD_RECEIVE_TIMES)
        public abstract Builder receiveTimes(ReceiveTimes receiveTimes);

        public abstract ProcessingStatusSummary build();
    }

    @AutoValue
    @JsonDeserialize(builder = ReceiveTimes.Builder.class)
    public static abstract class ReceiveTimes {
        public static final String FIELD_INGEST = "ingest";
        public static final String FIELD_POST_PROCESSING = "post_processing";
        public static final String FIELD_POST_INDEXING = "post_indexing";

        @JsonProperty(FIELD_INGEST)
        public abstract DateTime ingest();

        @JsonProperty(FIELD_POST_PROCESSING)
        public abstract DateTime postProcessing();

        @JsonProperty(FIELD_POST_INDEXING)
        public abstract DateTime postIndexing();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_ProcessingStatusSummary_ReceiveTimes.Builder();
            }

            @JsonProperty(FIELD_INGEST)
            public abstract Builder ingest(DateTime timestamp);

            @JsonProperty(FIELD_POST_PROCESSING)
            public abstract Builder postProcessing(DateTime timestamp);

            @JsonProperty(FIELD_POST_INDEXING)
            public abstract Builder postIndexing(DateTime timestamp);

            public abstract ReceiveTimes build();
        }
    }
}
