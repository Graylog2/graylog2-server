package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MoreSearchAdapterES7Test {
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
        QueryBuilder lessThanOrEqualFilter = MoreSearchAdapterES7.buildExtraFilter(FIELD, value);
        assertInstanceOf(expectedFilterClass, lessThanOrEqualFilter);
        assertEquals(expectedFilter, lessThanOrEqualFilter);
    }
}
