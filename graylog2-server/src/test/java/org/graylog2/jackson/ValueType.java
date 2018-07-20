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
package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_ValueType.Builder.class)
public abstract class ValueType implements Parent {
    static final String VERSION = "1";
    private static final String FIELD_FOOBAR = "foobar";

    @JsonProperty(FIELD_FOOBAR)
    public abstract String foobar();

    public static Builder builder() {
        return new AutoValue_ValueType.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder implements Parent.ParentBuilder<Builder> {
        @JsonProperty(FIELD_FOOBAR)
        public abstract Builder foobar(String foobar);

        abstract ValueType autoBuild();

        public ValueType build() {
            version(VERSION);
            return autoBuild();
        }
    }
}