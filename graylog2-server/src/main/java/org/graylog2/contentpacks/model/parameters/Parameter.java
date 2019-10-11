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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.entities.references.ValueType;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = Parameter.FIELD_TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanParameter.class, name = BooleanParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = DoubleParameter.class, name = DoubleParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = FloatParameter.class, name = FloatParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = IntegerParameter.class, name = IntegerParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = LongParameter.class, name = LongParameter.TYPE_NAME),
        @JsonSubTypes.Type(value = StringParameter.class, name = StringParameter.TYPE_NAME),
})
public interface Parameter<T> {
    String FIELD_TYPE = "type";
    String FIELD_NAME = "name";
    String FIELD_TITLE = "title";
    String FIELD_DESCRIPTION = "description";
    String FIELD_DEFAULT_VALUE = "default_value";

    @JsonProperty(FIELD_NAME)
    String name();

    @JsonProperty(FIELD_TITLE)
    String title();

    @JsonProperty(FIELD_DESCRIPTION)
    String description();

    @JsonProperty(FIELD_TYPE)
    ValueType valueType();

    @JsonProperty(FIELD_DEFAULT_VALUE)
    Optional<T> defaultValue();

    interface ParameterBuilder<SELF, T> {
        @JsonProperty(FIELD_NAME)
        SELF name(String name);

        @JsonProperty(FIELD_TITLE)
        SELF title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        SELF description(String description);

        @JsonProperty(FIELD_TYPE)
        SELF valueType(ValueType type);

        // It would be nicer to use
        //     SELF defaultValue(T defaultValue);
        // but this isn't possible until the following issues have been fixed:
        // https://github.com/google/auto/issues/627
        // https://github.com/google/auto/pull/515
        @JsonProperty(FIELD_DEFAULT_VALUE)
        SELF defaultValue(Optional defaultValue);
    }
}
