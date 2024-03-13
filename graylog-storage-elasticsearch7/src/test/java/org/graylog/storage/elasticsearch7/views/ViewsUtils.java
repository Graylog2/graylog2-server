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
package org.graylog.storage.elasticsearch7.views;

import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

public class ViewsUtils {
    static List<String> indicesOf(List<SearchRequest> clientRequest) {
        return clientRequest.stream()
                .map(request -> String.join(",", request.indices()))
                .collect(Collectors.toList());
    }

    public static ESGeneratedQueryContext.Factory createTestContextFactory(FieldTypesLookup fieldTypesLookup) {
        return (elasticsearchBackend, ssb, errors, timezone) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, errors, timezone, fieldTypesLookup);
    }

    public static ESGeneratedQueryContext.Factory createTestContextFactory() {
        return createTestContextFactory(mock(FieldTypesLookup.class));
    }

    public static ESGeneratedQueryContext createTestContext(ElasticsearchBackend backend) {
        return new ESGeneratedQueryContext(backend, new SearchSourceBuilder(), Collections.emptyList(), DateTimeZone.UTC, mock(FieldTypesLookup.class));
    }
}
