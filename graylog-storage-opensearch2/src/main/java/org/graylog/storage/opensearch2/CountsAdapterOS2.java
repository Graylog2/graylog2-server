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

import jakarta.inject.Inject;
import org.graylog2.indexer.counts.CountsAdapter;
import org.opensearch.client.opensearch.core.MsearchRequest;

import java.util.List;

public class CountsAdapterOS2 implements CountsAdapter {
    private final OpenSearchClient client;

    @Inject
    public CountsAdapterOS2(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public long totalCount(List<String> indices) {
        final var request = new MsearchRequest.Builder()
                .searches(searchBuilder -> searchBuilder
                        .header(headerBuilder -> headerBuilder.index(indices))
                        .body(bodyBuilder -> bodyBuilder
                                .query(queryBuilder -> queryBuilder.matchAll(matchAll -> matchAll))
                                .size(0)
                                .trackTotalHits(totalHits -> totalHits.enabled(true))))
                .build();
        final var result = client.search(request, "Fetching message count failed for indices ");

        return result.result().hits().total().value();
    }
}
