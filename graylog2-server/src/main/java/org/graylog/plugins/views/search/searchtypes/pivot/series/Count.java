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
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.TypedBuilder;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonTypeName(Count.NAME)
@JsonDeserialize(builder = Count.Builder.class)
public abstract class Count implements SeriesSpec {
    public static final String NAME = "count";
    @Override
    public abstract String type();

    @Override
    public abstract String id();

    @Nullable
    @JsonProperty
    public abstract String field();

    public static Builder builder() {
        return new AutoValue_Count.Builder().type(NAME);
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends TypedBuilder<Count, Builder> {
        @JsonCreator
        public static Builder create() {
            return Count.builder();
        }

        @JsonProperty
        public abstract Builder id(@Nullable String id);

        @JsonProperty
        public abstract Builder field(@Nullable String field);

        abstract Optional<String> id();
        abstract String field();
        abstract Count autoBuild();

        public Count build() {
            if (!id().isPresent()) {
                id(NAME + "(" + Strings.nullToEmpty(field()) + ")");
            }
            return autoBuild();
        }
    }
}
