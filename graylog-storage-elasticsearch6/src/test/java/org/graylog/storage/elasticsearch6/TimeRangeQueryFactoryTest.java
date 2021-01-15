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
package org.graylog.storage.elasticsearch6;

import org.assertj.core.api.Assertions;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.RangeQueryBuilder;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeRangeQueryFactoryTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @BeforeClass
    public static void initialize() {
        DateTimeUtils.setCurrentMillisFixed(new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC).getMillis());
    }

    @AfterClass
    public static void shutdown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void getTimestampRangeFilterReturnsNullIfTimeRangeIsNull() {
        Assertions.assertThat(TimeRangeQueryFactory.create(null)).isNull();
    }

    @Test
    public void getTimestampRangeFilterReturnsRangeQueryWithGivenTimeRange() {
        final DateTime from = new DateTime(2016, 1, 15, 12, 0, DateTimeZone.UTC);
        final DateTime to = from.plusHours(1);
        final TimeRange timeRange = AbsoluteRange.create(from, to);
        final RangeQueryBuilder queryBuilder = (RangeQueryBuilder) TimeRangeQueryFactory.create(timeRange);
        assertThat(queryBuilder).isNotNull();
        assertThat(queryBuilder.fieldName()).isEqualTo("timestamp");
        assertThat(queryBuilder.from()).isEqualTo(Tools.buildElasticSearchTimeFormat(from));
        assertThat(queryBuilder.to()).isEqualTo(Tools.buildElasticSearchTimeFormat(to));
    }
}
