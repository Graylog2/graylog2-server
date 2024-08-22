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
package org.graylog.storage.opensearch2.views;

import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
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

    public static OSGeneratedQueryContext.Factory createTestContextFactory(FieldTypesLookup fieldTypesLookup) {
        return (elasticsearchBackend, ssb, errors, timezone) -> new OSGeneratedQueryContext(elasticsearchBackend, ssb, errors, timezone, fieldTypesLookup);
    }

    public static OSGeneratedQueryContext.Factory createTestContextFactory() {
        return createTestContextFactory(mock(FieldTypesLookup.class));
    }

    public static OSGeneratedQueryContext createTestContext(OpenSearchBackend backend) {
        return new OSGeneratedQueryContext(backend, new SearchSourceBuilder(), Collections.emptyList(), DateTimeZone.UTC, mock(FieldTypesLookup.class));
    }
}
