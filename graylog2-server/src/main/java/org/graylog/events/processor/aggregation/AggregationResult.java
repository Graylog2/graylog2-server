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