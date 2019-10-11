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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = SeriesConfigDTO.Builder.class)
@WithBeanGetter
public abstract class SeriesConfigDTO {
    static final String FIELD_NAME = "name";

    public static SeriesConfigDTO empty() {
        return Builder.builder().build();
    }

    @JsonProperty(FIELD_NAME)
    @Nullable
    public abstract String name();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_NAME)
        @Nullable
        public abstract Builder name(String name);

        public abstract SeriesConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_SeriesConfigDTO.Builder();
        }
    }
}
