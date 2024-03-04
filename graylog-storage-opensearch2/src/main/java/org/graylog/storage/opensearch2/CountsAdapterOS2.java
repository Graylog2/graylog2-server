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

import java.util.List;

public class CountsAdapterOS2 implements CountsAdapter {
    private final OpenSearchClient client;

    @Inject
    public CountsAdapterOS2(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public long totalCount(List<String> indices) {
        final var result = client.execute(c -> c.search(searchBuilder -> searchBuilder
                        .query(queryBuilder -> queryBuilder.matchAll(matchAll -> matchAll))
                        .size(0)
                        .trackTotalHits(totalHits -> totalHits.enabled(true))
                        .index(indices), IndexedMessage.class),
                "Fetching message count failed for indices ");

        return result.hits().total().value();
    }
}
