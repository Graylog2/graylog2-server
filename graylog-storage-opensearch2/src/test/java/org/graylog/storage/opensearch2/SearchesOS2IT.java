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

import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesIT;
import org.junit.Rule;

public class SearchesOS2IT extends SearchesIT {
    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected SearchServerInstance elasticsearch() {
        return this.openSearchInstance;
    }

    private SearchesAdapter createSearchesAdapter() {
        final ScrollResultOS2.Factory scrollResultFactory = (initialResult, query, scroll, fields, limit) -> new ScrollResultOS2(
                openSearchInstance.elasticsearchClient(), initialResult, query, scroll, fields, limit
        );
        final SortOrderMapper sortOrderMapper = new SortOrderMapper();
        final boolean allowHighlighting = true;
        final boolean allowLeadingWildcardSearches = true;

        final SearchRequestFactory searchRequestFactory = new SearchRequestFactory(sortOrderMapper, allowHighlighting, allowLeadingWildcardSearches);
        return new SearchesAdapterOS2(openSearchInstance.elasticsearchClient(),
                new Scroll(openSearchInstance.elasticsearchClient(),
                        scrollResultFactory,
                        searchRequestFactory),
                searchRequestFactory);
    }

    @Override
    public Searches createSearches() {
        return new Searches(
                indexRangeService,
                metricRegistry,
                streamService,
                indices,
                indexSetRegistry,
                createSearchesAdapter()
        );
    }
}
