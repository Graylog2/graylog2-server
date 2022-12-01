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

import com.google.common.collect.Streams;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog2.indexer.results.ChunkedQueryResult;
import org.graylog2.indexer.results.ResultMessage;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ChunkedQueryResultOS2 extends ChunkedQueryResult<OpenSearchClient, SearchResponse> {

    public ChunkedQueryResultOS2(OpenSearchClient client, SearchResponse initialResult, String query, List<String> fields, int limit) {
        super(client, initialResult, query, fields, limit);
    }

    @Override
    protected List<ResultMessage> collectMessagesFromResult(SearchResponse result) {
        return Streams.stream(result.getHits())
                .map(hit -> ResultMessage.parseFromSource(hit.getId(), hit.getIndex(), hit.getSourceAsMap()))
                .collect(Collectors.toList());
    }

    @Override
    protected long countTotalHits(SearchResponse response) {
        return response.getHits().getTotalHits().value;
    }

    @Override
    protected long getTookMillisFromResponse(SearchResponse response) {
        return response.getTook().getMillis();
    }

}

