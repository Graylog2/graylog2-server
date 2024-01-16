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
package org.graylog2.indexer.searches.timerangepresets.conversion;


import org.graylog2.indexer.searches.timerangepresets.TimerangePreset;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.Period;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TimerangeOptionsToTimerangePresetsConversion {

    private final Function<Period, RelativeRange> periodConverter;

    @Inject
    public TimerangeOptionsToTimerangePresetsConversion(final PeriodToRelativeRangeConverter periodConverter) {
        this.periodConverter = periodConverter;
    }

    public List<TimerangePreset> convert(final Map<Period, String> timerangeOptions) {
        if (timerangeOptions == null) {
            return List.of();
        }
        return timerangeOptions.entrySet()
                .stream()
                .map(entry -> new TimerangePreset(
                        periodConverter.apply(entry.getKey()),
                        entry.getValue())
                )
                .collect(Collectors.toList());
    }


}
