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
package org.graylog.plugins.views.search.views.formatting.highlighting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = HighlightingRule.Builder.class)
@WithBeanGetter
public abstract class HighlightingRule {
    static final String FIELD_FIELD = "field";
    static final String FIELD_VALUE = "value";
    static final String FIELD_CONDITION = "condition";
    static final String FIELD_COLOR = "color";

    @JsonProperty(FIELD_FIELD)
    public abstract String field();

    @JsonProperty(FIELD_VALUE)
    public abstract String value();

    @JsonProperty(FIELD_COLOR)
    public abstract String color();


    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_FIELD)
        public abstract Builder field(String field);
        @JsonProperty(FIELD_VALUE)
        public abstract Builder value(String value);
        @JsonProperty(FIELD_COLOR)
        public abstract Builder color(String color);

        public abstract HighlightingRule build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_HighlightingRule.Builder();
        }
    }
}
