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
package org.graylog2.lookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_LookupDefaultSingleValue.Builder.class)
@AutoValue
public abstract class LookupDefaultSingleValue implements LookupDefaultValue {
    @Override
    @JsonProperty(FIELD_VALUE_STRING)
    public abstract String valueString();

    @Override
    @JsonProperty(FIELD_VALUE_TYPE)
    public abstract LookupDefaultValue.Type valueType();

    @Override
    @JsonProperty(FIELD_VALUE)
    @Nullable
    public abstract Object value();

    @Override
    public boolean isSet() {
        return valueType() != Type.NULL;
    }

    public static LookupDefaultSingleValue create(String valueString, LookupDefaultValue.Type valueType) {
        requireNonNull(valueString, "valueString cannot be null");
        requireNonNull(valueType, "valueType cannot be null");

        Object value;
        try {
            switch (valueType) {
                case STRING:
                    value = valueString;
                    break;
                case NUMBER:
                    value = OBJECT_MAPPER.convertValue(valueString, Number.class);
                    break;
                case BOOLEAN:
                    value = Boolean.parseBoolean(valueString);
                    break;
                case NULL:
                    value = null;
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert <" + valueString + "> to single value type <" + valueType + ">");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse JSON "+ valueType.toString().toLowerCase(Locale.ENGLISH) + " value <" + valueString + ">", e);
        }

        return builder()
                .valueString(valueString)
                .valueType(valueType)
                .value(value)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_LookupDefaultSingleValue.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_VALUE_STRING)
        public abstract Builder valueString(String valueString);

        @JsonProperty(FIELD_VALUE_TYPE)
        public abstract Builder valueType(LookupDefaultValue.Type valueType);

        @JsonProperty(FIELD_VALUE)
        public abstract Builder value(@Nullable Object value);

        public abstract LookupDefaultSingleValue build();
    }
}