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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = WidgetPositionDTO.Builder.class)
@WithBeanGetter
public abstract class WidgetPositionDTO {
    @JsonProperty("col")
    public abstract Position col();

    @JsonProperty("row")
    public abstract Position row();

    @JsonProperty("height")
    public abstract Position height();

    @JsonProperty("width")
    public abstract Position width();

    public static Builder builder() {
        return new AutoValue_WidgetPositionDTO.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("col")
        public abstract Builder col(Position col);

        @JsonProperty("row")
        public abstract Builder row(Position row);

        @JsonProperty("height")
        public abstract Builder height(Position height);

        @JsonProperty("width")
        public abstract Builder width(Position width);

        public abstract WidgetPositionDTO build();

        @JsonCreator
        public static Builder create() { return new AutoValue_WidgetPositionDTO.Builder(); }
    }
}
