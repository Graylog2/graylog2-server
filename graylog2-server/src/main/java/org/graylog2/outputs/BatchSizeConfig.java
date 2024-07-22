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
package org.graylog2.outputs;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.converters.IntegerConverter;
import com.github.joschi.jadconfig.converters.SizeConverter;
import com.github.joschi.jadconfig.util.Size;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;

import java.util.Optional;

public class BatchSizeConfig {
    private final String value;

    public BatchSizeConfig(String value) {
        this.value = value;
    }

    public Optional<Size> getAsBytes() {
        try {
            return Optional.of(new SizeConverter().convertFrom(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Optional<Integer> getAsCount() {
        try {
            return Optional.of(new IntegerConverter().convertFrom(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Provides backwards compatibility for {@link org.graylog2.configuration.ExposedConfiguration}.
     * If the value is a number, keep serializing it as a number. Otherwise, just use the originally configured string.
     */
    @JsonValue
    public Object jsonValue() {
        if (getAsCount().isPresent()) {
            return getAsCount();
        }
        return value;
    }

    public static class Validator implements com.github.joschi.jadconfig.Validator<BatchSizeConfig> {
        @Override
        public void validate(String name, BatchSizeConfig config) throws ValidationException {
            try {
                final Size size = Size.parse(config.value);
                if (size.toMegabytes() > 90) {
                    throw new ValidationException(
                            "Parameter <%s> should not be greater than 90MB. (Found <%s>)".formatted(name, size));
                }
            } catch (Exception ignored) {
                final Integer intValue = new IntegerConverter().convertFrom(config.value);
                new PositiveIntegerValidator().validate(name, intValue);
            }
        }
    }

    public static class Converter implements com.github.joschi.jadconfig.Converter<BatchSizeConfig> {
        @Override
        public BatchSizeConfig convertFrom(String value) {
            return new BatchSizeConfig(value);
        }

        @Override
        public String convertTo(BatchSizeConfig value) {
            return value.value;
        }
    }
}
