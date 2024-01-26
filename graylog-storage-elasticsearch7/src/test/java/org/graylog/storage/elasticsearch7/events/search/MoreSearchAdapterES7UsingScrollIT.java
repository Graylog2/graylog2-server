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
package org.graylog.storage.elasticsearch7.events.search;

import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.events.search.MoreSearchAdapterIT;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.MoreSearchAdapterES7;
import org.graylog.storage.elasticsearch7.Scroll;
import org.graylog.storage.elasticsearch7.ScrollResultES7;
import org.graylog.storage.elasticsearch7.SearchRequestFactory;
import org.graylog.storage.elasticsearch7.SortOrderMapper;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.junit.Rule;

public class MoreSearchAdapterES7UsingScrollIT extends MoreSearchAdapterIT {

    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected SearchServerInstance searchServer() {
        return elasticsearch;
    }

    @Override
    protected MoreSearchAdapter createMoreSearchAdapter() {
        final SortOrderMapper sortOrderMapper = new SortOrderMapper();
        final ElasticsearchClient client = elasticsearch.elasticsearchClient();
        return new MoreSearchAdapterES7(client, true, sortOrderMapper,
                new Scroll(client,
                        (initialResult, query, scroll, fields, limit) -> new ScrollResultES7(
                                client, initialResult, query, scroll, fields, limit
                        ),
                        new SearchRequestFactory(sortOrderMapper, false, true, new IgnoreSearchFilters())
                )

        );
    }
}
