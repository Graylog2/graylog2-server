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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.util.Size;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;

import java.util.Objects;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * This class represents the configuration of a batch size which is measured in bytes but also supports a count based
 * configuration, e.g. number of messages, for backwards compatibility.
 */
public class BatchSizeConfig {
    private final String value;
    private final Size size;
    private final Integer count;

    /**
     * Create a config by parsing the supplied string as a {@link Size}, or an {@link Integer} for backwards
     * compatibility.
     */
    @JsonCreator
    public static BatchSizeConfig parse(String value) {
        try {
            return new BatchSizeConfig(value, Size.parse(value), null);
        } catch (IllegalArgumentException sizeParsingException) {
            try {
                return new BatchSizeConfig(value, null, Integer.valueOf(value));
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException(f("\"%s\" is neither a size [%s] nor an integer.", value,
                        sizeParsingException.getLocalizedMessage()));
            }
        }
    }

    /**
     * Create a count based config for backwards compatibility.
     */
    @JsonCreator
    public static BatchSizeConfig forCount(int count) {
        return new BatchSizeConfig(String.valueOf(count), null, count);
    }

    private BatchSizeConfig(String value, Size size, Integer count) {
        this.value = value;
        this.size = size;
        this.count = count;
    }

    public Optional<Size> getAsBytes() {
        return Optional.ofNullable(size);
    }

    public Optional<Integer> getAsCount() {
        return Optional.ofNullable(count);
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
            if (config.size != null && config.size.toMegabytes() > 99) {
                throw new ValidationException(
                        "Parameter <%s> must not be greater than 99 MB. (Found <%s>)".formatted(name, config.size));
            }
            if (config.count != null) {
                new PositiveIntegerValidator().validate(name, config.count);
            }
        }
    }

    public static class Converter implements com.github.joschi.jadconfig.Converter<BatchSizeConfig> {
        @Override
        public BatchSizeConfig convertFrom(String value) {
            try {
                return BatchSizeConfig.parse(value);
            } catch (IllegalArgumentException e) {
                throw new ParameterException(f("Couldn't convert value \"%s\" to batch size config.", value), e);
            }
        }

        @Override
        public String convertTo(BatchSizeConfig value) {
            return value.value;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof final BatchSizeConfig that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
