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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.ClearScrollRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchScrollRequest;
import org.graylog.shaded.opensearch2.org.opensearch.common.unit.TimeValue;

import java.io.IOException;
import java.util.List;

public class ScrollResultOS2 extends ChunkedQueryResultOS2 {
    private static final TimeValue DEFAULT_SCROLL = TimeValue.timeValueMinutes(1);

    private final String scroll;

    public interface Factory {
        ScrollResultOS2 create(SearchResponse initialResult, @Assisted("query") String query, @Assisted("scroll") String scroll, List<String> fields, int limit);
    }

    @AssistedInject
    public ScrollResultOS2(OpenSearchClient client,
                           @Assisted SearchResponse initialResult,
                           @Assisted("query") String query,
                           @Assisted("scroll") String scroll,
                           @Assisted List<String> fields,
                           @Assisted int limit) {
        super(client, initialResult, query, fields, limit);
        this.scroll = scroll;

    }

    @Override
    protected SearchResponse nextSearchResult() throws IOException {
        if (this.lastSearchResponse.getScrollId() == null) {
            //with ignore_unavailable=true and no available indices, response does not contain scrollId
            return null;
        }
        final SearchScrollRequest scrollRequest = new SearchScrollRequest(this.lastSearchResponse.getScrollId());
        scrollRequest.scroll(TimeValue.parseTimeValue(this.scroll, DEFAULT_SCROLL, "scroll time"));
        return client.executeWithIOException((c, requestOptions) -> c.scroll(scrollRequest, requestOptions),
                "Unable to retrieve next chunk from search: ");
    }

    @Override
    public void cancel() throws IOException {
        if (this.lastSearchResponse.getScrollId() == null) {
            //with ignore_unavailable=true and no available indices, response does not contain scrollId, there is nothing to cancel
            return;
        }
        final ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(this.lastSearchResponse.getScrollId());

        client.executeWithIOException((c, requestOptions) -> c.clearScroll(request, requestOptions),
                "Unable to cancel scrolling search request");
    }

    @Override
    protected String getChunkingMethodName() {
        return "scroll";
    }

}
