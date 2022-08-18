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
import org.joda.time.Period;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class JavaDurationConverter implements Converter<Duration> {
    @Override
    public Duration convertFrom(String value) {
        // ISO8601 format
        if (value.startsWith("P")) {
            final Period period = Period.parse(value);
            return Duration.parse(period.toString());
        }
        // number + unit formats
        final com.github.joschi.jadconfig.util.Duration jadDuration = com.github.joschi.jadconfig.util.Duration.parse(value);
        final ChronoUnit chronoUnit = toChronoUnit(jadDuration.getUnit());
        return Duration.of(jadDuration.getQuantity(), chronoUnit);
    }

    @Override
    public String convertTo(Duration value) {
        // Durations will always be converted to ISO8601 formatted Strings
        // There is no meaningful way to convert them back to a simple jadconfig 'number+unit' format.
        return value.toString();
    }

    public static ChronoUnit toChronoUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case HOURS:
                return ChronoUnit.HOURS;
            case DAYS:
                return ChronoUnit.DAYS;
            default:
                throw new AssertionError();
        }
    }
}
