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
package org.graylog.plugins.views.search.searchtypes.pivot.series;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import java.util.Optional;

@AutoValue
@JsonTypeName(Max.NAME)
@JsonDeserialize(builder = Max.Builder.class)
public abstract class Max implements SeriesSpec {
    public static final String NAME = "max";
    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @JsonProperty
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_Max.Builder().type(NAME);
    }

    @AutoValue.Builder
    public abstract static class Builder extends TypedBuilder<Max, Builder> {
        @JsonCreator
        public static Builder create() { return Max.builder(); }

        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty
        public abstract Builder field(String field);

        abstract Optional<String> id();
        abstract String field();
        abstract Max autoBuild();

        public Max build() {
            if (!id().isPresent()) {
                id(NAME + "(" + field() + ")");
            }
            return autoBuild();
        }
    }
}
