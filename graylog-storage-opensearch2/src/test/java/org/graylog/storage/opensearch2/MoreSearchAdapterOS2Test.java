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
package org.graylog.storage.opensearch2;

import org.graylog.shaded.opensearch2.org.opensearch.index.query.MultiMatchQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.RangeQueryBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MoreSearchAdapterOS2Test {
    private static final String FIELD = "field";
    private static final String VALUE = "100";

    @Test
    void testBuildExtraFilter() {
        verifyFilter("<=100", QueryBuilders.rangeQuery(FIELD).lte(VALUE), RangeQueryBuilder.class);
        verifyFilter(">=100", QueryBuilders.rangeQuery(FIELD).gte(VALUE), RangeQueryBuilder.class);
        verifyFilter("<100", QueryBuilders.rangeQuery(FIELD).lt(VALUE), RangeQueryBuilder.class);
        verifyFilter(">100", QueryBuilders.rangeQuery(FIELD).gt(VALUE), RangeQueryBuilder.class);
        verifyFilter(VALUE, QueryBuilders.multiMatchQuery(VALUE, FIELD), MultiMatchQueryBuilder.class);
    }

    private static void verifyFilter(String value, QueryBuilder expectedFilter, Class<? extends QueryBuilder> expectedFilterClass) {
        QueryBuilder lessThanOrEqualFilter = MoreSearchAdapterOS2.buildExtraFilter(FIELD, value);
        assertInstanceOf(expectedFilterClass, lessThanOrEqualFilter);
        assertEquals(expectedFilter, lessThanOrEqualFilter);
    }
}
