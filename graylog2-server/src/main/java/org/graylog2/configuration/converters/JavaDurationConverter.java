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
package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.jodatime.converters.DurationConverter;
import org.joda.time.Period;

import java.time.Duration;

public class JavaDurationConverter implements Converter<Duration> {
    @Override
    public Duration convertFrom(String value) {
        // ISO8601 format
        if (value.startsWith("P")) {
            final Period period = Period.parse(value);
            return Duration.parse(period.toString());
        }
        // number + unit formats
        final org.joda.time.Duration jodaDuration = new DurationConverter().convertFrom(value);
        return Duration.parse(jodaDuration.toString());
    }

    @Override
    public String convertTo(Duration value) {
        // Durations will always be converted to ISO8601 formatted Strings
        // There is no meaningful way to convert them back to a simple jadconfig 'number+unit' format.
        return value.toString();
    }
}
