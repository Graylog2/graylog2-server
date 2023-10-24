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
    public RelativeRange apply(Period period) {
        if (period != null) {
            // years are not supported in the toStandardSeconds conversion, so we convert it to days and assume 365 days a year
            if(period.getYears() > 0) {
                final int years = period.getYears();
                period = period.minusYears(years);
                period = period.plusDays(years * 365);
            }

            // months are not supported in the toStandardSeconds conversion, so we convert it to days and assume 30 days a month
            if(period.getMonths() > 0) {
                final int months = period.getMonths();
                period = period.minusMonths(months);
                period = period.plusDays(months * 30);
            }

            return RelativeRange.Builder.builder()
                    .from(period.toStandardSeconds().getSeconds())
                    .build();
        } else {
            return null;
        }
    }
}
