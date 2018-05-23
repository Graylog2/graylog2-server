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
package org.graylog2.contentpacks.model.entities.references;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.contentpacks.model.parameters.FilledParameter;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_ValueReference.Builder.class)
public abstract class ValueReference implements ValueTyped, Reference {
    public static final String FIELD_VALUE = "value";

    @JsonProperty(FIELD_VALUE)
    public abstract Object value();

    public Boolean asBoolean(Map<String, FilledParameter<?>> parameters) {
        switch (valueType()) {
            case BOOLEAN:
                return Boolean.class.cast(value());
            case PARAMETER:
                return asType(parameters, Boolean.class);
            default:
                throw new IllegalStateException("Expected value reference of type BOOLEAN but got " + valueType());
        }
    }

    public Double asDouble(Map<String, FilledParameter<?>> parameters) {
        switch (valueType()) {
            case DOUBLE:
                return Double.class.cast(value());
            case PARAMETER:
                return asType(parameters, Double.class);
            default:
                throw new IllegalStateException("Expected value reference of type DOUBLE but got " + valueType());
        }
    }

    public Float asFloat(Map<String, FilledParameter<?>> parameters) {
        switch (valueType()) {
            case FLOAT:
                return Float.class.cast(value());
            case PARAMETER:
                return asType(parameters, Float.class);
            default:
                throw new IllegalStateException("Expected value reference of type FLOAT but got " + valueType());
        }
    }

    public Integer asInteger(Map<String, FilledParameter<?>> parameters) {
        switch (valueType()) {
            case INTEGER:
                return Integer.class.cast(value());
            case PARAMETER:
                return asType(parameters, Integer.class);
            default:
                throw new IllegalStateException("Expected value reference of type INTEGER but got " + valueType());
        }
    }

    public Long asLong(Map<String, FilledParameter<?>> parameters) {
        switch (valueType()) {
            case LONG:
                return Long.class.cast(value());
            case PARAMETER:
                return asType(parameters, Long.class);
            default:
                throw new IllegalStateException("Expected value reference of type LONG but got " + valueType());
        }
    }

    public String asString(Map<String, FilledParameter<?>> parameters) {
        switch (valueType()) {
            case STRING:
                return String.class.cast(value());
            case PARAMETER:
                return asType(parameters, String.class);
            default:
                throw new IllegalStateException("Expected value reference of type STRING but got " + valueType());
        }
    }

    public String asString() {
        switch (valueType()) {
            case STRING:
                return String.class.cast(value());
            default:
                throw new IllegalStateException("Expected value reference of type STRING but got " + valueType());
        }
    }

    private <S> S asType(Map<String, FilledParameter<?>> parameters, Class<S> type) {
        if (valueType() == ValueType.PARAMETER) {
            final String value = String.class.cast(value());
            final FilledParameter<?> filledParameter = parameters.get(value);
            if (filledParameter.valueType().targetClass().equals(type)) {
                return type.cast(filledParameter.getValue());
            } else {
                throw new IllegalStateException("Expected parameter reference for Java type " + type + " but got " + filledParameter.valueType());
            }
        }
        throw new IllegalStateException("Expected value reference of type PARAMETER but got " + valueType());
    }

    public <S extends Enum<S>> S asEnum(Map<String, FilledParameter<?>> parameters, Class<S> type) {
        final String value;
        switch (valueType()) {
            case STRING:
                value = String.class.cast(value());
                break;
            case PARAMETER:
                value = asType(parameters, String.class);
                break;
            default:
                throw new IllegalStateException("Expected value reference of type STRING or PARAMETER but got " + valueType());
        }

        return Enum.valueOf(type, value);
    }

    @Nullable
    public static ValueReference of(Object value) {
        if (value instanceof Boolean) {
            return of((Boolean) value);
        } else if (value instanceof Float) {
            return of((Float) value);
        } else if (value instanceof Integer) {
            return of((Integer) value);
        } else if (value instanceof String) {
            return of((String) value);
        } else if (value instanceof Enum) {
            return of((Enum) value);
        } else {
            return null;
        }
    }

    public static ValueReference of(Boolean value) {
        return ValueReference.builder()
                .valueType(ValueType.BOOLEAN)
                .value(value)
                .build();
    }

    public static ValueReference of(Double value) {
        return ValueReference.builder()
                .valueType(ValueType.DOUBLE)
                .value(value)
                .build();
    }

    public static ValueReference of(Float value) {
        return ValueReference.builder()
                .valueType(ValueType.FLOAT)
                .value(value)
                .build();
    }

    public static ValueReference of(Integer value) {
        return ValueReference.builder()
                .valueType(ValueType.INTEGER)
                .value(value)
                .build();
    }

    public static ValueReference of(Long value) {
        return ValueReference.builder()
                .valueType(ValueType.LONG)
                .value(value)
                .build();
    }

    public static ValueReference of(String value) {
        return ValueReference.builder()
                .valueType(ValueType.STRING)
                .value(value)
                .build();
    }

    public static ValueReference of(Enum value) {
        return ValueReference.builder()
                .valueType(ValueType.STRING)
                .value(value.name())
                .build();
    }

    public static ValueReference createParameter(String value) {
        return ValueReference.builder()
                .valueType(ValueType.PARAMETER)
                .value(value)
                .build();
    }

    public static ValueReference.Builder builder() {
        return new AutoValue_ValueReference.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements TypeBuilder<Builder> {
        @JsonProperty(FIELD_VALUE)
        public abstract Builder value(Object value);

        abstract ValueReference autoBuild();

        public ValueReference build() {
            final ValueReference valueReference = autoBuild();
            final boolean isParameter = valueReference.valueType() == ValueType.PARAMETER;
            if (isParameter) {
                final String value = (String) valueReference.value();
                Preconditions.checkArgument(StringUtils.isNotBlank(value), "Parameter must not be blank");
            }
            return valueReference;
        }
    }
}
