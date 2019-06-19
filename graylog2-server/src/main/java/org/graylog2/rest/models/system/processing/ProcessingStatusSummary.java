/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.models.system.processing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;

@AutoValue
@JsonDeserialize(builder = ProcessingStatusSummary.Builder.class)
public abstract class ProcessingStatusSummary {
    public static final String FIELD_MAX_RECEIVE_TIMES = "max_receive_times";

    @JsonProperty(FIELD_MAX_RECEIVE_TIMES)
    public abstract MaxReceiveTimes maxReceiveTimes();

    public static Builder builder() {
        return Builder.create();
    }

    public static ProcessingStatusSummary of(ProcessingStatusRecorder processingStatusRecorder) {
        return builder()
                .maxReceiveTimes(MaxReceiveTimes.builder()
                        .preJournal(processingStatusRecorder.getPreJournalMaxReceiveTime())
                        .postProcessing(processingStatusRecorder.getPostProcessingMaxReceiveTime())
                        .postIndexing(processingStatusRecorder.getPostIndexingMaxReceiveTime())
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

        @JsonProperty(FIELD_MAX_RECEIVE_TIMES)
        public abstract Builder maxReceiveTimes(MaxReceiveTimes maxReceiveTimes);

        public abstract ProcessingStatusSummary build();
    }

    @AutoValue
    @JsonDeserialize(builder = MaxReceiveTimes.Builder.class)
    public static abstract class MaxReceiveTimes {
        public static final String FIELD_PRE_JOURNAL = "pre_journal";
        public static final String FIELD_POST_PROCESSING = "post_processing";
        public static final String FIELD_POST_INDEXING = "post_indexing";

        @JsonProperty(FIELD_PRE_JOURNAL)
        public abstract DateTime preJournal();

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
                return new AutoValue_ProcessingStatusSummary_MaxReceiveTimes.Builder();
            }

            @JsonProperty(FIELD_PRE_JOURNAL)
            public abstract Builder preJournal(DateTime timestamp);

            @JsonProperty(FIELD_POST_PROCESSING)
            public abstract Builder postProcessing(DateTime timestamp);

            @JsonProperty(FIELD_POST_INDEXING)
            public abstract Builder postIndexing(DateTime timestamp);

            public abstract MaxReceiveTimes build();
        }
    }
}