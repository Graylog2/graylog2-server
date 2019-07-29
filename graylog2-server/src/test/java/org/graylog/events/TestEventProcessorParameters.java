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
package org.graylog.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.processor.EventProcessorParametersWithTimerange;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@AutoValue
@JsonTypeName(TestEventProcessorParameters.TYPE_NAME)
@JsonDeserialize(builder = TestEventProcessorParameters.Builder.class)
public abstract class TestEventProcessorParameters implements EventProcessorParametersWithTimerange {
    public static final String TYPE_NAME = "__test_event_processor_parameters__";

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static TestEventProcessorParameters create(DateTime from, DateTime to) {
        return builder().timerange(AbsoluteRange.create(from, to)).build();
    }

    @Override
    public EventProcessorParametersWithTimerange withTimerange(DateTime from, DateTime to) {
        requireNonNull(from, "from cannot be null");
        requireNonNull(to, "to cannot be null");
        checkArgument(to.isAfter(from), "to must be after from");

        return builder().timerange(AbsoluteRange.create(from, to)).build();
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventProcessorParametersWithTimerange.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TestEventProcessorParameters.Builder();
        }

        abstract TestEventProcessorParameters autoBuild();

        public TestEventProcessorParameters build() {
            type(TYPE_NAME);
            return autoBuild();
        }
    }
}