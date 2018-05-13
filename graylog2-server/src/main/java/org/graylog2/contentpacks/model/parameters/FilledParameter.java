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
package org.graylog2.contentpacks.model.parameters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.entities.references.ValueTyped;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, property = FilledParameter.FIELD_TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FilledBooleanParameter.class, name = FilledBooleanParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = FilledDoubleParameter.class, name = FilledDoubleParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = FilledFloatParameter.class, name = FilledFloatParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = FilledIntegerParameter.class, name = FilledIntegerParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = FilledLongParameter.class, name = FilledLongParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = FilledStringParameter.class, name = FilledStringParameter.TYPE_NAME),
})
public interface FilledParameter<T> extends ValueTyped {
    String FIELD_NAME = "name";
    String FIELD_VALUE = "value";
    String FIELD_DEFAULT_VALUE = "default_value";

    @JsonProperty(FIELD_NAME)
    String name();

    @JsonProperty(FIELD_VALUE)
    Optional<T> value();

    @JsonProperty(FIELD_DEFAULT_VALUE)
    Optional<T> defaultValue();

    @JsonIgnore
    default T getValue() {
        return value().orElseGet(
                () -> defaultValue().orElseThrow(
                        () -> new IllegalStateException("Missing default value for parameter \"" + name() + "\" of type " + valueType())));
    }

    interface ParameterBuilder<SELF, T> extends TypeBuilder<SELF> {
        @JsonProperty(FIELD_NAME)
        SELF name(String name);

        // It would be nicer to use
        //     SELF value(T value);
        // but this isn't possible until the following issues have been fixed:
        // https://github.com/google/auto/issues/627
        // https://github.com/google/auto/pull/515
        @JsonProperty(FIELD_VALUE)
        SELF value(Optional value);

        // It would be nicer to use
        //     SELF defaultValue(T defaultValue);
        // but this isn't possible until the following issues have been fixed:
        // https://github.com/google/auto/issues/627
        // https://github.com/google/auto/pull/515
        @JsonProperty(FIELD_DEFAULT_VALUE)
        SELF defaultValue(Optional defaultValue);
    }
}
