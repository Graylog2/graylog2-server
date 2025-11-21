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
package org.graylog.storage.opensearch3.views.export;

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.plugin.Message;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import java.util.List;
import java.util.Map;

public class SearchAfter implements RequestStrategy {

    static final String DEFAULT_TIEBREAKER_FIELD = Message.GL2_SECOND_SORT_FIELD;

    private final OfficialOpensearchClient opensearchClient;

    private List<FieldValue> searchAfterValues = null;

    @Inject
    public SearchAfter(OfficialOpensearchClient opensearchClient) {
        this.opensearchClient = opensearchClient;
    }

    @Override
    public List<Hit<Map>> nextChunk(org.opensearch.client.opensearch.core.SearchRequest newSearch, ExportMessagesCommand command) {
        org.opensearch.client.opensearch.core.SearchResponse<Map> newResult = search(newSearch);
        final List<Hit<Map>> newHits = newResult.hits().hits();
        searchAfterValues = lastHitSortFrom(newHits);
        return newHits;
    }

    private SearchResponse<Map> search(org.opensearch.client.opensearch.core.SearchRequest newSearch) {
        final SearchResponse<Map> result = opensearchClient.sync(c -> c.search(newSearch, Map.class), "failed to serarch");
        if (result.shards().failed() > 0) {
            final List<String> errors = result.shards().failures().stream()
                    .map(e -> e.reason().reason())
                    .distinct()
                    .toList();
            throw new ElasticsearchException("Unable to perform export query: ", errors);
        }
        return result;
    }

    private List<FieldValue> lastHitSortFrom(List<Hit<Map>> hits) {
        if (hits.isEmpty()) {
            return null;
        }
        return hits.getLast().sort();
    }

    @Override
    public void configure(org.opensearch.client.opensearch.core.SearchRequest.Builder builder) {
        if (searchAfterValues != null) {
            builder.searchAfter(searchAfterValues);
        }
    }
}
