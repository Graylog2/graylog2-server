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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.ClearScrollRequest;
import org.opensearch.client.opensearch.core.ScrollRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScrollResultOS3 extends ChunkedQueryResultOS {
    private static final String DEFAULT_SCROLL = "1m";

    private final String scroll;

    public interface Factory {
        ScrollResultOS3 create(SearchResponse<Map> initialResult, @Assisted("query") String query, @Assisted("scroll") String scroll, List<String> fields, int limit);
    }

    @AssistedInject
    public ScrollResultOS3(ResultMessageFactory resultMessageFactory,
                           OfficialOpensearchClient client,
                           @Assisted SearchResponse<Map> initialResult,
                           @Assisted("query") String query,
                           @Assisted("scroll") String scroll,
                           @Assisted List<String> fields,
                           @Assisted int limit) {
        super(resultMessageFactory, client, initialResult, query, fields, limit);
        this.scroll = scroll;
    }

    @Override
    @Nullable
    protected SearchResponse<Map> nextSearchResult() {
        final String currentScrollId = this.lastSearchResponse.scrollId();
        System.out.println("DEBUG: nextSearchResult called, scrollId=" + currentScrollId);

        if (currentScrollId == null) {
            // with ignore_unavailable=true and no available indices, response does not contain scrollId
            System.out.println("DEBUG: scrollId is null, returning null");
            return null;
        }

        final String scrollTime = Optional.ofNullable(this.scroll)
                .filter(s -> !s.isEmpty())
                .orElse(DEFAULT_SCROLL);

        final Time time = new Time.Builder().time(scrollTime).build();
        System.out.println("DEBUG: Creating scroll request with scrollTime=" + scrollTime + ", time.time()=" + time.time());
        final ScrollRequest scrollRequest = ScrollRequest.of(sr -> sr
                .scrollId(currentScrollId)
                .scroll(time));

        final SearchResponse<Map> response = client.sync(
                c -> c.scroll(scrollRequest, Map.class),
                "Unable to retrieve next chunk from search: "
        );

        System.out.println("DEBUG: Got scroll response, hits=" + (response != null ? response.hits().hits().size() : "null"));
        return response;
    }

    @Override
    public void cancel() {
        if (this.lastSearchResponse.scrollId() == null) {
            // with ignore_unavailable=true and no available indices, response does not contain scrollId, there is nothing to cancel
            return;
        }

        final ClearScrollRequest clearRequest = ClearScrollRequest.of(csr -> csr
                .scrollId(List.of(this.lastSearchResponse.scrollId())));

        client.execute(
                () -> client.sync(c -> c.clearScroll(clearRequest), "Unable to cancel scrolling search request"),
                "Unable to cancel scrolling search request"
        );
    }

    @Override
    protected String getChunkingMethodName() {
        return "scroll";
    }

}
