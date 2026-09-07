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
package org.graylog.storage.opensearch3.views.searchtypes.pivot.buckets;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.InlineScript;

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

    private InlineScript scriptForFields(final List<String> fields) {
        final Values bucketSpec = Values.builder().fields(fields).limit(10).build();
        final Pivot pivot = mock(Pivot.class);
        when(pivot.sort()).thenReturn(List.of());

        final BucketSpecHandler.CreatedAggregations<MutableNamedAggregationBuilder> aggregations =
                handler.doCreateAggregation(BucketSpecHandler.Direction.Row, "agg-name", pivot, bucketSpec,
                        mock(OSGeneratedQueryContext.class), mock(Query.class));

        return aggregations.leaf().build().terms().script().inline();
    }

    @Test
    void passesFieldNamesAsScriptParameters() {
        final InlineScript script = scriptForFields(List.of("source", "http.response.status_code"));

        assertThat(script.source())
                .doesNotContain("source")
                .doesNotContain("http.response.status_code");
        assertThat(script.params()).containsOnlyKeys("field0", "field1", "missingValue");
        assertThat(script.params().get("field0").to(String.class)).isEqualTo("source");
        assertThat(script.params().get("field1").to(String.class)).isEqualTo("http.response.status_code");
    }

    @Test
    void generatesTheSameScriptSourceRegardlessOfTheFieldNames() {
        assertThat(scriptForFields(List.of("source")).source())
                .isEqualTo(scriptForFields(List.of("gl2_source_input")).source());
    }
}
