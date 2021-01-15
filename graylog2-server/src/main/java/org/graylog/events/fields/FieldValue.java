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
package org.graylog.events.fields;

import com.google.auto.value.AutoValue;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

import java.util.Optional;

@AutoValue
public abstract class FieldValue {
    public abstract FieldValueType dataType();

    public abstract String value();

    public static FieldValue create(FieldValueType dataType, String value) {
        return builder()
                .dataType(dataType)
                .value(value)
                .build();
    }

    public static FieldValue error() {
        return create(FieldValueType.ERROR, "");
    }

    public static FieldValue string(String value) {
        return create(FieldValueType.STRING, value);
    }

    public Optional<Long> longValue(String value) {
        if (dataType().validate(value).isPresent()) {
            // TODO: Do something in case of error?
            return Optional.empty();
        }
        return Optional.ofNullable(Longs.tryParse(value));
    }

    public Optional<Double> doubleValue(String value) {
        if (dataType().validate(value).isPresent()) {
            // TODO: Do something in case of error?
            return Optional.empty();
        }
        return Optional.ofNullable(Doubles.tryParse(value));
    }

    public Optional<Boolean> booleanValue(String value) {
        if (dataType().validate(value).isPresent()) {
            // TODO: Do something in case of error?
            return Optional.empty();
        }
        return Optional.of(Boolean.valueOf(value));
    }

    public boolean isError() {
        return dataType().isError();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public static Builder create() {
            return new AutoValue_FieldValue.Builder();
        }

        public abstract Builder dataType(FieldValueType dataType);

        public abstract Builder value(String value);

        public abstract FieldValue build();
    }
}