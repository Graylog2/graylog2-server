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
package org.graylog.plugins.views.search.engine;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationRequestTest {

    @Test
    void combineQueryFilterQueryOnly() {
        final String q = builder()
                .query(ElasticsearchQueryString.of("foo:bar"))
                .build()
                .getCombinedQueryWithFilter();

        assertThat(q).isEqualTo("foo:bar");
    }

    @Test
    void combineQueryFilterBoth() {
        final String q = builder()
                .query(ElasticsearchQueryString.of("foo:bar"))
                .filter(ElasticsearchQueryString.of("lorem:ipsum"))
                .build()
                .getCombinedQueryWithFilter();

        assertThat(q).isEqualTo("foo:bar AND lorem:ipsum");
    }

    private ValidationRequest.Builder builder() {
        return ValidationRequest.builder()
                .timerange(RelativeRange.create(300))
                .streams(Collections.emptySet())
                .parameters(ImmutableSet.<Parameter>builder().build());
    }
}
