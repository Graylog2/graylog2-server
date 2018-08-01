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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.Optional;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class DashboardWidgetEntity {
    @JsonProperty("id")
    @NotNull
    public abstract ValueReference id();

    @JsonProperty("description")
    @NotNull
    public abstract ValueReference description();

    @JsonProperty("type")
    @NotBlank
    public abstract ValueReference type();

    @JsonProperty("cache_time")
    @PositiveOrZero
    public abstract ValueReference cacheTime();

    @JsonProperty("time_range")
    @NotNull
    public abstract TimeRangeEntity timeRange();

    @JsonProperty("configuration")
    @NotNull
    public abstract ReferenceMap configuration();

    @JsonProperty("position")
    public abstract Optional<Position> position();

    @JsonCreator
    public static DashboardWidgetEntity create(
            @JsonProperty("id") @NotNull ValueReference id,
            @JsonProperty("description") @NotNull ValueReference description,
            @JsonProperty("type") @NotBlank ValueReference type,
            @JsonProperty("cache_time") @PositiveOrZero ValueReference cacheTime,
            @JsonProperty("time_range") @NotNull TimeRangeEntity timeRange,
            @JsonProperty("configuration") @NotNull ReferenceMap configuration,
            @JsonProperty("position") @Nullable Position position) {
        return new AutoValue_DashboardWidgetEntity(id, description, type, cacheTime, timeRange, configuration, Optional.ofNullable(position));
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    public static abstract class Position {
        @JsonProperty("width")
        @PositiveOrZero
        public abstract ValueReference width();

        @JsonProperty("height")
        @PositiveOrZero
        public abstract ValueReference height();

        @JsonProperty("row")
        @PositiveOrZero
        public abstract ValueReference row();

        @JsonProperty("col")
        @PositiveOrZero
        public abstract ValueReference col();

        @JsonCreator
        public static Position create(@JsonProperty("width") @PositiveOrZero ValueReference width,
                                      @JsonProperty("height") @PositiveOrZero ValueReference height,
                                      @JsonProperty("row") @PositiveOrZero ValueReference row,
                                      @JsonProperty("col") @PositiveOrZero ValueReference col) {
            return new AutoValue_DashboardWidgetEntity_Position(width, height, row, col);
        }
    }
}
