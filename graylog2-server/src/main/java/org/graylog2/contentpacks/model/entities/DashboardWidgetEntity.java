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
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.Map;
import java.util.Optional;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class DashboardWidgetEntity {
    @JsonProperty("description")
    @NotNull
    public abstract String description();

    @JsonProperty("type")
    @NotBlank
    public abstract String type();

    @JsonProperty("cache_time")
    @PositiveOrZero
    public abstract int cacheTime();

    @JsonProperty("time_range")
    @NotNull
    public abstract TimeRange timeRange();

    @JsonProperty("configuration")
    @NotNull
    public abstract Map<String, Object> configuration();

    @JsonProperty("position")
    public abstract Optional<Position> position();

    @JsonCreator
    public static DashboardWidgetEntity create(
            @JsonProperty("description") @NotNull String description,
            @JsonProperty("type") @NotBlank String type,
            @JsonProperty("cache_time") @PositiveOrZero int cacheTime,
            @JsonProperty("time_range") @NotNull TimeRange timeRange,
            @JsonProperty("configuration") @NotNull Map<String, Object> configuration,
            @JsonProperty("position") Position position) {
        return new AutoValue_DashboardWidgetEntity(description, type, cacheTime, timeRange, configuration, Optional.ofNullable(position));
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    public static abstract class Position {
        @JsonProperty("width")
        @PositiveOrZero
        public abstract int width();

        @JsonProperty("height")
        @PositiveOrZero
        public abstract int height();

        @JsonProperty("row")
        @PositiveOrZero
        public abstract int row();

        @JsonProperty("col")
        @PositiveOrZero
        public abstract int col();

        @JsonCreator
        public static Position create(@JsonProperty("width") @PositiveOrZero int width,
                                      @JsonProperty("height") @PositiveOrZero int height,
                                      @JsonProperty("row") @PositiveOrZero int row,
                                      @JsonProperty("col") @PositiveOrZero int col) {
            return new AutoValue_DashboardWidgetEntity_Position(width, height, row, col);
        }
    }
}
