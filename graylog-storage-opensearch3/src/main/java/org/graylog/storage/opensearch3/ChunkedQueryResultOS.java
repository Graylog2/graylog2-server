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
package org.graylog.storage.opensearch3;

import org.graylog2.indexer.results.ChunkedQueryResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ChunkedQueryResultOS extends ChunkedQueryResult<OfficialOpensearchClient, SearchResponse<Map>> {

    private final ResultMessageFactory resultMessageFactory;

    public ChunkedQueryResultOS(ResultMessageFactory resultMessageFactory, OfficialOpensearchClient client,
                                SearchResponse<Map> initialResult, String query, List<String> fields, int limit) {
        super(client, initialResult, query, fields, limit);
        this.resultMessageFactory = resultMessageFactory;
    }

    @Override
    protected List<ResultMessage> collectMessagesFromResult(SearchResponse<Map> result) {
        return result.hits().hits().stream()
                .map(hit -> resultMessageFactory.parseFromSource(hit.id(), hit.index(), hit.source()))
                .collect(Collectors.toList());
    }

    @Override
    protected long countTotalHits(SearchResponse<Map> response) {
        return response.hits().total().value();
    }

    @Override
    protected long getTookMillisFromResponse(SearchResponse<Map> response) {
        return response.took();
    }

}
