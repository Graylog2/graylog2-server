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
