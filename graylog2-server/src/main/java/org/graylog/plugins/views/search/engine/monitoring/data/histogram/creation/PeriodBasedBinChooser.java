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
package org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation;

import org.graylog.plugins.views.search.engine.QueryExecutionStats;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PeriodBasedBinChooser implements BinChooser<Period, QueryExecutionStats> {

    @Override
    public Optional<Period> chooseBin(final List<Period> availablePeriods, final QueryExecutionStats stats) {
        return availablePeriods.stream()
                .filter(per -> matches(per, stats.effectiveTimeRange()))
                .findFirst();
    }


    private boolean matches(final Period binRange, final TimeRange statsRange) {
        final Duration statsRangeAsDuration = new Duration(statsRange.getFrom().toInstant(), statsRange.getTo().toInstant());
        return binRange.toStandardDuration().compareTo(statsRangeAsDuration) >= 0;
    }

    @Override
    public Optional<Comparator<Period>> getBinComparator() {
        return Optional.of(Comparator.comparing(Period::toStandardDuration));
    }
}
