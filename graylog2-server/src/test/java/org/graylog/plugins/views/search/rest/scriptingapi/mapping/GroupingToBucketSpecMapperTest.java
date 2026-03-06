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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import jakarta.validation.ValidationException;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.NumberRange;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.RangeBucket;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupingToBucketSpecMapperTest {

    private GroupingToBucketSpecMapper toTest;

    @BeforeEach
    void setUp() {
        toTest = new GroupingToBucketSpecMapper();
    }

    @Test
    void throwsNullPointerExceptionOnNullGrouping() {
        assertThrows(NullPointerException.class, () -> toTest.apply(null));
    }

    @Test
    void buildsBucketSpecCorrectly() {
        Grouping grouping = new Grouping("source", 3);
        final BucketSpec bucketSpec = toTest.apply(grouping);

        assertThat(bucketSpec)
                .isNotNull()
                .isInstanceOf(Values.class)
                .satisfies(b -> assertEquals(Collections.singletonList("source"), b.fields()))
                .satisfies(b -> assertEquals(Values.NAME, b.type()))
                .satisfies(b -> assertEquals(3, ((Values) b).limit()));
    }

    @Test
    void usesDefaultLimitIfWrongLimitProvided() {
        Grouping grouping = new Grouping("source", -42);
        final BucketSpec bucketSpec = toTest.apply(grouping);

        assertThat(bucketSpec)
                .isNotNull()
                .isInstanceOf(Values.class)
                .satisfies(b -> assertEquals(Collections.singletonList("source"), b.fields()))
                .satisfies(b -> assertEquals(Values.NAME, b.type()))
                .satisfies(b -> assertEquals(Values.DEFAULT_LIMIT, ((Values) b).limit()));
    }

    @Test
    void buildsRangeBucketSpecCorrectly() {
        final List<NumberRange> ranges = List.of(
                new NumberRange(null, 100.0),
                new NumberRange(100.0, 500.0),
                new NumberRange(500.0, null)
        );
        final Grouping grouping = new Grouping("took_ms", Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(ranges));
        final BucketSpec bucketSpec = toTest.apply(grouping);

        assertThat(bucketSpec)
                .isNotNull()
                .isInstanceOf(RangeBucket.class)
                .satisfies(b -> assertEquals(Collections.singletonList("took_ms"), b.fields()))
                .satisfies(b -> assertEquals(RangeBucket.NAME, b.type()))
                .satisfies(b -> assertEquals(3, ((RangeBucket) b).ranges().size()));
    }

    @Test
    void throwsValidationExceptionWhenRangesAndLimitBothProvided() {
        final List<NumberRange> ranges = List.of(new NumberRange(null, 100.0));
        assertThrows(ValidationException.class,
                () -> new Grouping("field", Optional.of(10), Optional.empty(), Optional.empty(), Optional.of(ranges)));
    }

    @Test
    void throwsValidationExceptionWhenRangesAndTimeunitBothProvided() {
        final List<NumberRange> ranges = List.of(new NumberRange(null, 100.0));
        assertThrows(ValidationException.class,
                () -> new Grouping("field", Optional.empty(), Optional.of("1h"), Optional.empty(), Optional.of(ranges)));
    }

    @Test
    void throwsValidationExceptionWhenRangesAndScalingBothProvided() {
        final List<NumberRange> ranges = List.of(new NumberRange(null, 100.0));
        assertThrows(ValidationException.class,
                () -> new Grouping("field", Optional.empty(), Optional.empty(), Optional.of(1.0), Optional.of(ranges)));
    }

}
