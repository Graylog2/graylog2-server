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
package org.graylog.storage.elasticsearch7.views.export;

import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;

public interface RequestStrategy {
    List<SearchHit> nextChunk(SearchRequest search, ExportMessagesCommand command);

    /**
     * Allows implementers to specify options on SearchSourceBuilder that cannot be specified on Search.Builder.
     *
     * @see #nextChunk(SearchRequest, ExportMessagesCommand)
     * @see org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder#searchAfter(Object[])
     */
    default SearchSourceBuilder configure(SearchSourceBuilder ssb) {
        return ssb;
    }
}
