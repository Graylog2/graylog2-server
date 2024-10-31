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
package org.graylog2.plugin.indexer.searches.timeranges;

import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.graylog.testing.jackson.JacksonSubtypesAssertions.assertThatDto;

class TimeRangeTest {
    @Test
    void subtypes() {
        final var now = DateTime.now(DateTimeZone.UTC);

        final var absoluteRange = AbsoluteRange.create(now, now);
        final var relativeRange = RelativeRange.create(500);
        final var keywordRange = KeywordRange.create("yesterday", "UTC");
        final var offsetRange = OffsetRange.Builder.builder()
                .offset(1)
                .source("foo")
                .build();

        assertThatDto(absoluteRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
        assertThatDto(relativeRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
        assertThatDto(keywordRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
        assertThatDto(offsetRange)
                .doesNotSerializeWithDuplicateFields()
                .deserializesWhenGivenSupertype(TimeRange.class);
    }
}
