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
package org.graylog.plugins.views.search.timeranges;

import org.graylog.plugins.views.search.Query;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DerivedTimeRangeTest {
    private static final Query emptyRootQuery = Query.emptyRoot();

    @Test
    public void returnsInitialRangeForRelativeRange() throws Exception {
        final RelativeRange range = RelativeRange.create(300);
        final DerivedTimeRange derivedTimeRange = DerivedTimeRange.of(range);

        assertThat(derivedTimeRange.effectiveTimeRange(emptyRootQuery, null)).isEqualTo(range);
    }

    @Test
    public void returnsInitialRangeForAbsoluteRange() throws Exception {
        final AbsoluteRange range = AbsoluteRange.create("2019-11-18T10:00:00.000Z", "2019-11-21T12:00:00.000Z");
        final DerivedTimeRange derivedTimeRange = DerivedTimeRange.of(range);

        assertThat(derivedTimeRange.effectiveTimeRange(emptyRootQuery, null)).isEqualTo(range);
    }

    @Test
    public void returnsInitialRangeForKeywordRange() throws Exception {
        final KeywordRange range = KeywordRange.create("yesterday");
        final DerivedTimeRange derivedTimeRange = DerivedTimeRange.of(range);

        assertThat(derivedTimeRange.effectiveTimeRange(emptyRootQuery, null)).isEqualTo(range);
    }

    @Test
    public void callsDeriveTimeRangeIfOffsetRange() throws Exception {
        final OffsetRange range = mock(OffsetRange.class);
        final AbsoluteRange resultRange = AbsoluteRange.create("2019-11-18T10:00:00.000Z", "2019-11-21T12:00:00.000Z");

        when(range.type()).thenReturn(OffsetRange.OFFSET);
        when(range.deriveTimeRange(any(), any())).thenReturn(resultRange);
        final DerivedTimeRange derivedTimeRange = DerivedTimeRange.of(range);

        assertThat(derivedTimeRange.effectiveTimeRange(emptyRootQuery, null)).isEqualTo(resultRange);
    }
}
