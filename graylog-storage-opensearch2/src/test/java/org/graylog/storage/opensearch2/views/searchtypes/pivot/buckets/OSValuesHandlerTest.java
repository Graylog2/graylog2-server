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
package org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.shaded.opensearch2.org.opensearch.script.Script;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OSValuesHandlerTest {
    private OSValuesHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OSValuesHandler();
    }

    private Script scriptForFields(final List<String> fields) {
        final Values bucketSpec = Values.builder().fields(fields).limit(10).build();
        final Pivot pivot = mock(Pivot.class);
        when(pivot.sort()).thenReturn(List.of());

        final BucketSpecHandler.CreatedAggregations<AggregationBuilder> aggregations = handler.doCreateAggregation(
                BucketSpecHandler.Direction.Row, "agg-name", pivot, bucketSpec,
                mock(OSGeneratedQueryContext.class), mock(Query.class));

        assertThat(aggregations.leaf()).isInstanceOf(TermsAggregationBuilder.class);
        return ((TermsAggregationBuilder) aggregations.leaf()).script();
    }

    @Test
    void passesFieldNamesAsScriptParameters() {
        final Script script = scriptForFields(List.of("source", "http.response.status_code"));

        assertThat(script.getIdOrCode())
                .doesNotContain("source")
                .doesNotContain("http.response.status_code");
        assertThat(script.getParams())
                .containsEntry("field0", "source")
                .containsEntry("field1", "http.response.status_code");
    }

    @Test
    void generatesTheSameScriptSourceRegardlessOfTheFieldNames() {
        assertThat(scriptForFields(List.of("source")).getIdOrCode())
                .isEqualTo(scriptForFields(List.of("gl2_source_input")).getIdOrCode());
    }
}
