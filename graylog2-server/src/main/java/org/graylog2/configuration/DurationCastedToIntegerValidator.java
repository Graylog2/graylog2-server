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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.util.Duration;

public class DurationCastedToIntegerValidator implements Validator<Duration> {
    private static final Duration maximumValue = Duration.days(24);

    /**
     * Validates if the value {@literal value} of the provided configuration parameter {@literal name} is a positive
     * {@link Duration}, that is not higher than 24 days. This constraint is related to the duration (in milliseconds)
     * being casted to integer at some point. {@link Integer#MAX_VALUE} / (24 * 60 * 60 * 1000) roughly equals to 24 days.
     *
     * @param name  The name of the configuration parameter
     * @param value The value of the configuration validator
     * @throws ValidationException If the value {@literal value} configuration parameter {@literal name} can't be parsed
     *                             as a {@link Duration}, is negative or higher than 24 days.
     */
    @Override
    public void validate(String name, Duration value) throws ValidationException {
        if (value != null && (value.getQuantity() < 0L || value.toMilliseconds() > maximumValue.toMilliseconds())) {
            throw new ValidationException("Parameter " + name + " should be positive and not higher than 24 days (found " + value + ")");
        }
    }
}
