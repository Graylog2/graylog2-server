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
package org.graylog2.indexer.searches;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.jodatime.api.Assertions.assertThat;

public class IndexRangeStatsTest {
    @Test
    public void testCreate() throws Exception {
        DateTime min = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime max = new DateTime(2015, 1, 3, 0, 0, DateTimeZone.UTC);
        IndexRangeStats indexRangeStats = IndexRangeStats.create(min, max);

        assertThat(indexRangeStats.min()).isEqualTo(min);
        assertThat(indexRangeStats.max()).isEqualTo(max);
    }

    @Test
    public void testEmptyInstance() throws Exception {
        assertThat(IndexRangeStats.EMPTY.min()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(IndexRangeStats.EMPTY.max()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }
}