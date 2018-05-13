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
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_RelativeRange.Builder.class)
public abstract class RelativeRange extends TimeRangeEntity {
    static final String TYPE = "relative";
    private static final String FIELD_RANGE = "range";

    @JsonProperty(FIELD_RANGE)
    public abstract ValueReference range();

    public static RelativeRange of(org.graylog2.plugin.indexer.searches.timeranges.RelativeRange relativeRange) {
        final int range = relativeRange.getRange();
        return builder()
                .range(ValueReference.of(range))
                .build();
    }

    static RelativeRange.Builder builder() {
        return new AutoValue_RelativeRange.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder implements TimeRangeBuilder<Builder> {
        @JsonProperty(FIELD_RANGE)
        abstract Builder range(ValueReference range);

        abstract RelativeRange autoBuild();

        RelativeRange build() {
            type(ModelType.of(TYPE));
            return autoBuild();
        }
    }
}
