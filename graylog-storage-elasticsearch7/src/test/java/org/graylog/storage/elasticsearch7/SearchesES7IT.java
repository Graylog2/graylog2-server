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
package org.graylog.storage.elasticsearch7;

import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.indexer.searches.SearchesIT;
import org.junit.Rule;

public class SearchesES7IT extends SearchesIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    private SearchesAdapter createSearchesAdapter() {
        final ScrollResultES7.Factory scrollResultFactory = (initialResult, query, scroll, fields, limit) -> new ScrollResultES7(
                elasticsearch.elasticsearchClient(), initialResult, query, scroll, fields, limit
        );
        final SortOrderMapper sortOrderMapper = new SortOrderMapper();
        final boolean allowHighlighting = true;
        final boolean allowLeadingWildcardSearches = true;

        final SearchRequestFactory searchRequestFactory = new SearchRequestFactory(sortOrderMapper, allowHighlighting, allowLeadingWildcardSearches);
        return new SearchesAdapterES7(elasticsearch.elasticsearchClient(),
                new Scroll(elasticsearch.elasticsearchClient(),
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
