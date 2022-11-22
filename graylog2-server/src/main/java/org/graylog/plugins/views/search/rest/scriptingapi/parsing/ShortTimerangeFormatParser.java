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
package org.graylog.plugins.views.search.rest.scriptingapi.parsing;

import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class ShortTimerangeFormatParser {

    private static final Pattern SHORT_FORMAT_PATTERN = Pattern.compile("^[0-9]+[a-zA-Z]$");
    private static final Map<String, String> SHORT_TO_LONG_PERIOD_MAPPING = Map.of(
            "s", "seconds",
            "m", "minutes",
            "h", "hours",
            "d", "days",
            "w", "weeks",
            "M", "months",
            "y", "years"
    );

    public Optional<TimeRange> parse(final String shortTimerange) {
        if (shortTimerange != null && SHORT_FORMAT_PATTERN.matcher(shortTimerange).matches()) {
            final String numberPart = shortTimerange.substring(0, shortTimerange.length() - 1);
            final String periodPart = shortTimerange.substring(shortTimerange.length() - 1);
            String longPeriodPart = SHORT_TO_LONG_PERIOD_MAPPING.get(periodPart);
            if (longPeriodPart != null) {
                if ("1".equals(numberPart)) {
                    longPeriodPart = longPeriodPart.substring(0, longPeriodPart.length() - 1); //removing last "s"
                }
                return Optional.of(
                        KeywordRange.create(
                                "last " + numberPart + " " + longPeriodPart,
                                "UTC")
                );
            }
        }
        return Optional.empty();

    }
}
