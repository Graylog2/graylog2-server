/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.contentpacks.model.entities.references;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_ValueReference.Builder.class)
public abstract class ValueReference implements Reference {
    public static final String FIELD_TYPE = "@type";
    public static final String FIELD_VALUE = "@value";

    @JsonProperty(FIELD_TYPE)
    public abstract ValueType valueType();

    @JsonProperty(FIELD_VALUE)
    public abstract Object value();

    public Boolean asBoolean(Map<String, ValueReference> parameters) {
        switch (valueType()) {
            case BOOLEAN:
                return Boolean.class.cast(value());
            case PARAMETER:
                return asType(parameters, Boolean.class);
            default:
                throw new IllegalStateException("Expected value reference of type BOOLEAN but got " + valueType());
        }
    }

    public Double asDouble(Map<String, ValueReference> parameters) {
        switch (valueType()) {
            case DOUBLE:
                if (value() instanceof Number) {
                    return ((Number)value()).doubleValue();
                }
                throw new IllegalStateException("Expected value reference of type DOUBLE but got " + value().getClass());
            case PARAMETER:
                return asType(parameters, Double.class);
            default:
                throw new IllegalStateException("Expected value reference of type DOUBLE but got " + valueType());
        }
    }

    public Float asFloat(Map<String, ValueReference> parameters) {
        switch (valueType()) {
            case FLOAT:
                if (value() instanceof Number) {
                    return ((Number)value()).floatValue();
                }
                throw new IllegalStateException("Expected value reference of type FLOAT but got " + value().getClass());
            case PARAMETER:
                return asType(parameters, Float.class);
            default:
                throw new IllegalStateException("Expected value reference of type FLOAT but got " + valueType());
        }
    }

    public Integer asInteger(Map<String, ValueReference> parameters) {
        switch (valueType()) {
            case INTEGER:
                return Integer.class.cast(value());
            case PARAMETER:
                return asType(parameters, Integer.class);
            default:
                throw new IllegalStateException("Expected value reference of type INTEGER but got " + valueType());
        }
    }

    public Long asLong(Map<String, ValueReference> parameters) {
        switch (valueType()) {
            case LONG:
                if (value() instanceof Number) {
                    return ((Number)value()).longValue();
                }
                throw new IllegalStateException("Expected value reference of type LONG but got " + value().getClass());
            case PARAMETER:
                return asType(parameters, Long.class);
            default:
                throw new IllegalStateException("Expected value reference of type LONG but got " + valueType());
        }
    }

    public String asString(Map<String, ValueReference> parameters) {
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

    private <S> S asType(Map<String, ValueReference> parameters, Class<S> type) {
        if (valueType() == ValueType.PARAMETER) {
            final String value = String.class.cast(value());
            final ValueReference filledParameter = parameters.get(value);
            if (filledParameter.valueType().targetClass().equals(type)) {
                return type.cast(filledParameter.value());
            } else {
                throw new IllegalStateException("Expected parameter reference for Java type " + type + " but got " + filledParameter.valueType());
            }
        }
        throw new IllegalStateException("Expected value reference of type PARAMETER but got " + valueType());
    }

    public <S extends Enum<S>> S asEnum(Map<String, ValueReference> parameters, Class<S> type) {
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
        } else if (value instanceof Double) {
            return of((Double) value);
        } else if (value instanceof Float) {
            return of((Float) value);
        } else if (value instanceof Integer) {
            return of((Integer) value);
        } else if (value instanceof Long) {
            return of((Long) value);
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
    public abstract static class Builder {
        @JsonProperty(FIELD_TYPE)
        public abstract Builder valueType(ValueType type);

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
