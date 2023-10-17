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
package org.graylog2.shared.utilities;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StringUtils {

    private StringUtils() {
    }

    /**
     * Calls {@link String#format(Locale, String, Object...)} with the given arguments with a locale value
     * of {@code Locale.ENGLISH}.
     *
     * @param format the format string
     * @param args   the format arguments
     * @return the formatted string
     */
    public static String f(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }

    /**
     * Calls {@link String#toUpperCase(Locale)} on the given string with a locale value of {@code Locale.ENGLISH}.
     *
     * @param value the value
     * @return the value in upper case
     * @throws IllegalArgumentException when given value is null
     */
    public static String toUpperCase(@Nullable String value) {
        if (value == null) {
            throw new IllegalArgumentException("String value cannot be null");
        }
        return value.toUpperCase(Locale.ENGLISH);
    }

    /**
     * Calls {@link String#toLowerCase(Locale)} on the given string with a locale value of {@code Locale.ENGLISH}.
     *
     * @param value the value
     * @return the value in lower case
     * @throws IllegalArgumentException when given value is null
     */
    public static String toLowerCase(@Nullable String value) {
        if (value == null) {
            throw new IllegalArgumentException("String value cannot be null");
        }
        return value.toLowerCase(Locale.ENGLISH);
    }

    public static String humanReadableByteCount(final long bytes) {
        final String[] units = {"B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};
        final int base = 1024;

        // When using the smallest unit no decimal point is needed, because it's the exact number.
        if (bytes < base) {
            return bytes + " " + units[0];
        }

        final int exponent = (int) (Math.log(bytes) / Math.log(base));
        final String unit = units[exponent];
        return StringUtils.f("%.1f %s", bytes / Math.pow(base, exponent), unit);
    }

    public static Set<String> splitByComma(Set<String> values) {
        return split(values).collect(Collectors.toSet());
    }

    public static List<String> splitByComma(List<String> values) {
        return split(values).collect(Collectors.toList());
    }

    private static Stream<String> split(Collection<String> values) {
        if (values == null) {
            return Stream.empty();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .flatMap(v -> Arrays.stream(v.split(",")))
                .filter(s -> !s.trim().isEmpty());
    }
}
