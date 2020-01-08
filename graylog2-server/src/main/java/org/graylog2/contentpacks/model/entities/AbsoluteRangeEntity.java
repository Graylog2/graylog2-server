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
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_AbsoluteRangeEntity.Builder.class)
public abstract class AbsoluteRangeEntity extends TimeRangeEntity {
    static final String TYPE = "absolute";
    private static final String FIELD_FROM = "from";
    private static final String FIELD_TO = "to";

    @JsonProperty(FIELD_FROM)
    public abstract ValueReference from();

    @JsonProperty(FIELD_TO)
    public abstract ValueReference to();

    public static AbsoluteRangeEntity of(AbsoluteRange absoluteRange) {
        final String from = absoluteRange.from().toString(ISODateTimeFormat.dateTime());
        final String to = absoluteRange.to().toString(ISODateTimeFormat.dateTime());
        return builder()
                .from(ValueReference.of(from))
                .to(ValueReference.of(to))
                .build();
    }

    static AbsoluteRangeEntity.Builder builder() {
        return new AutoValue_AbsoluteRangeEntity.Builder();
    }

    @Override
    public final TimeRange convert(Map<String, ValueReference> parameters) throws InvalidRangeParametersException {
        final String from = from().asString(parameters);
        final String to = to().asString(parameters);
        return AbsoluteRange.create(from, to);
    }

    @AutoValue.Builder
    abstract static class Builder implements TimeRangeBuilder<Builder> {
        @JsonProperty(FIELD_FROM)
        abstract Builder from(ValueReference from);

        @JsonProperty(FIELD_TO)
        abstract Builder to(ValueReference to);

        abstract AbsoluteRangeEntity autoBuild();

        AbsoluteRangeEntity build() {
            type(ModelTypeEntity.of(TYPE));
            return autoBuild();
        }
    }
}
