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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = PivotDTO.Builder.class)
@WithBeanGetter
public abstract class PivotDTO {
    static final String FIELD_FIELD_NAME = "field";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_FIELD_NAME)
    public abstract String field();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_CONFIG)
    public abstract PivotConfigDTO config();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_FIELD_NAME)
        public abstract Builder field(String field);

        @JsonProperty(FIELD_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = PivotDTO.FIELD_TYPE,
                visible = true)
        public abstract Builder config(PivotConfigDTO config);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        public abstract PivotDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_PivotDTO.Builder();
        }
    }
}
