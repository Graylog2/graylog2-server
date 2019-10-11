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
package org.graylog.events.fields;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.fields.providers.FieldValueProvider;

import java.util.List;

@AutoValue
@JsonDeserialize(builder = EventFieldSpec.Builder.class)
public abstract class EventFieldSpec {
    private static final String FIELD_DATA_TYPE = "data_type";
    private static final String FIELD_PROVIDERS = "providers";

    @JsonProperty(FIELD_DATA_TYPE)
    public abstract FieldValueType dataType();

    @JsonProperty(FIELD_PROVIDERS)
    public abstract List<FieldValueProvider.Config> providers();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventFieldSpec.Builder();
        }

        @JsonProperty(FIELD_DATA_TYPE)
        public abstract Builder dataType(FieldValueType dataType);

        @JsonProperty(FIELD_PROVIDERS)
        public abstract Builder providers(List<FieldValueProvider.Config> providers);

        public abstract EventFieldSpec build();
    }
}