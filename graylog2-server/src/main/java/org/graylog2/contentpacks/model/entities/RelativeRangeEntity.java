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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_RelativeRangeEntity.Builder.class)
public abstract class RelativeRangeEntity extends TimeRangeEntity {
    static final String TYPE = "relative";
    private static final String FIELD_RANGE = "range";

    @JsonProperty(FIELD_RANGE)
    public abstract ValueReference range();

    public static RelativeRangeEntity of(RelativeRange relativeRange) {
        final int range = relativeRange.getRange();
        return builder()
                .range(ValueReference.of(range))
                .build();
    }

    @Override
    public final TimeRange convert(Map<String, ValueReference> parameters) throws InvalidRangeParametersException {
        final int range = range().asInteger(parameters);
        return RelativeRange.create(range);
    }

    static RelativeRangeEntity.Builder builder() {
        return new AutoValue_RelativeRangeEntity.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder implements TimeRangeBuilder<Builder> {
        @JsonProperty(FIELD_RANGE)
        abstract Builder range(ValueReference range);

        abstract RelativeRangeEntity autoBuild();

        RelativeRangeEntity build() {
            type(ModelTypeEntity.of(TYPE));
            return autoBuild();
        }
    }
}
