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

import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Grouping grouping = new Grouping("source", 3, SortSpec.Direction.Ascending);
        final BucketSpec bucketSpec = toTest.apply(grouping);

        assertThat(bucketSpec)
                .isNotNull()
                .isInstanceOf(Values.class)
                .satisfies(b -> assertEquals("source", b.field()))
                .satisfies(b -> assertEquals(Values.NAME, b.type()))
                .satisfies(b -> assertEquals(3, ((Values) b).limit()));
    }

    @Test
    void usesDefaultLimitIfWrongLimitProvided() {
        Grouping grouping = new Grouping("source", -42, null);
        final BucketSpec bucketSpec = toTest.apply(grouping);

        assertThat(bucketSpec)
                .isNotNull()
                .isInstanceOf(Values.class)
                .satisfies(b -> assertEquals("source", b.field()))
                .satisfies(b -> assertEquals(Values.NAME, b.type()))
                .satisfies(b -> assertEquals(Values.DEFAULT_LIMIT, ((Values) b).limit()));
    }

}
