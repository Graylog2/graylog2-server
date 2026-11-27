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
package org.graylog2.configuration.managed;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the disk watermarks against each other, the way OpenSearch does when they are applied. Each value is valid on
 * its own, but OpenSearch refuses a combination which mixes disk usage and free space, or which isn't ordered from low
 * to flood stage. The user interface applies the same rule, see {@code configurationWatermarks.ts}.
 */
public final class DiskWatermarks {
    public static final String LOW = "cluster.routing.allocation.disk.watermark.low";
    public static final String HIGH = "cluster.routing.allocation.disk.watermark.high";
    public static final String FLOOD_STAGE = "cluster.routing.allocation.disk.watermark.flood_stage";
    public static final List<String> NAMES = List.of(LOW, HIGH, FLOOD_STAGE);

    private static final Pattern BYTE_SIZE = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)(b|kb|mb|gb|tb|pb)");
    private static final List<String> BYTE_UNITS = List.of("b", "kb", "mb", "gb", "tb", "pb");

    private enum Kind {USAGE, FREE_BYTES}

    private record Watermark(Kind kind, BigDecimal amount) {}

    private DiskWatermarks() {
    }

    /**
     * @param valueOf the value each watermark would have, {@code null} if unknown
     * @return why the combination is refused, empty if it is fine or can't be judged
     */
    public static Optional<String> check(Function<String, String> valueOf) {
        final List<Watermark> known = new ArrayList<>();
        for (final String name : NAMES) {
            final String value = valueOf.apply(name);
            if (value != null) {
                final Watermark watermark = parse(value.trim());
                if (watermark == null) {
                    // Rejected on its own already
                    return Optional.empty();
                }
                known.add(watermark);
            }
        }
        if (known.isEmpty()) {
            return Optional.empty();
        }
        final Kind kind = known.get(0).kind();
        if (known.stream().anyMatch(watermark -> watermark.kind() != kind)) {
            return Optional.of("Use disk usage for the low, high and flood stage disk watermarks, or free space for all three.");
        }
        for (int i = 1; i < known.size(); i++) {
            final int comparison = known.get(i - 1).amount().compareTo(known.get(i).amount());
            if (kind == Kind.USAGE ? comparison > 0 : comparison < 0) {
                return Optional.of(kind == Kind.USAGE
                        ? "Set the disk watermarks in ascending order: low, high, then flood stage."
                        : "Set free-space disk watermarks in descending order: low, high, then flood stage.");
            }
        }
        return Optional.empty();
    }

    /**
     * Convenience for a map of values, e.g. the effective ones.
     */
    public static Optional<String> check(Map<String, String> values) {
        return check(values::get);
    }

    @Nullable
    private static Watermark parse(String value) {
        try {
            if (value.endsWith("%")) {
                return new Watermark(Kind.USAGE, new BigDecimal(value.substring(0, value.length() - 1))
                        .movePointLeft(2));
            }
            final Matcher matcher = BYTE_SIZE.matcher(value.toLowerCase(Locale.ROOT));
            if (matcher.matches()) {
                final int exponent = BYTE_UNITS.indexOf(matcher.group(2));
                return new Watermark(Kind.FREE_BYTES,
                        new BigDecimal(matcher.group(1)).multiply(BigDecimal.valueOf(1024).pow(exponent)));
            }
            return new Watermark(Kind.USAGE, new BigDecimal(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
