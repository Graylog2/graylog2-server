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
package org.graylog.plugins.views.search;

import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryEffectiveTimeRangeTest {
    private Query query;

    @Before
    public void setUp() throws Exception {
        this.query = Query.emptyRoot();
    }

    @After
    public void tearDown() throws Exception {
        // Some tests modify the time so we make sure to reset it after each test even if assertions fail
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void returnQueryTimeRangeIfNoSearchTypeTimeRangeAndNoGlobalOverride() throws InvalidRangeParametersException {
        final SearchType searchType = mock(SearchType.class);
        when(searchType.timerange()).thenReturn(Optional.empty());
        final Query queryWithTimeRange = query.toBuilder().timerange(RelativeRange.create(3600)).build();

        final TimeRange result = queryWithTimeRange.effectiveTimeRange(searchType);

        assertThat(result).isEqualTo(RelativeRange.create(3600));
    }

    @Test
    public void returnSearchTypeTimeRangeIfPresentAndNoGlobalOverride() throws InvalidRangeParametersException {
        final SearchType searchType = mock(SearchType.class);
        when(searchType.timerange())
                .thenReturn(Optional.of(DerivedTimeRange.of(RelativeRange.create(7200))));
        final Query queryWithTimeRange = query.toBuilder().timerange(RelativeRange.create(3600)).build();

        final TimeRange result = queryWithTimeRange.effectiveTimeRange(searchType);

        assertThat(result).isEqualTo(RelativeRange.create(7200));
    }

    @Test
    public void returnGlobalOverrideTimeRangeIfPresent() throws InvalidRangeParametersException {
        final SearchType searchType = mock(SearchType.class);
        when(searchType.timerange())
                .thenReturn(Optional.of(DerivedTimeRange.of(RelativeRange.create(7200))));
        final Query queryWithTimeRange = query.toBuilder()
                .timerange(RelativeRange.create(3600))
                .globalOverride(GlobalOverride.builder()
                        .timerange(RelativeRange.create(600))
                        .build())
                .build();

        final TimeRange result = queryWithTimeRange.effectiveTimeRange(searchType);

        assertThat(result).isEqualTo(RelativeRange.create(600));
    }

    @Test
    public void returnGlobalOverrideTimeRangeWithOffsetIfPresentAndOffsetTimeRange() throws InvalidRangeParametersException {
        DateTimeUtils.setCurrentMillisFixed(1578590095642L);

        final SearchType searchType = mock(SearchType.class);
        when(searchType.timerange())
                .thenReturn(Optional.of(
                        DerivedTimeRange.of(
                                OffsetRange.Builder.builder()
                                        .source("query")
                                        .build()
                        )));
        final Query queryWithTimeRange = query.toBuilder()
                .timerange(RelativeRange.create(3600))
                .globalOverride(GlobalOverride.builder()
                        .timerange(RelativeRange.create(600))
                        .build())
                .build();

        final TimeRange result = queryWithTimeRange.effectiveTimeRange(searchType);

        assertThat(result).isEqualTo(AbsoluteRange.create("2020-01-09T16:54:55.642Z", "2020-01-09T17:04:55.642Z"));
    }
}
