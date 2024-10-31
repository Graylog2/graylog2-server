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

import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.Period;

import java.util.function.Function;

public class PeriodToRelativeRangeConverter implements Function<Period, RelativeRange> {

    @Override
    public RelativeRange apply(final Period period) {
        if (period != null) {
            return RelativeRange.Builder.builder()
                    .from(period.withYears(0).withMonths(0).plusDays(period.getYears() * 365).plusDays(period.getMonths() * 30).toStandardSeconds().getSeconds())
                    .build();
        } else {
            return null;
        }
    }
}
