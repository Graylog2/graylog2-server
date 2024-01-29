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
package org.graylog.storage.opensearch2.events.search;

import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.events.search.MoreSearchAdapterIT;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.storage.opensearch2.MoreSearchAdapterOS2;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog.storage.opensearch2.PaginationOS2;
import org.graylog.storage.opensearch2.SearchRequestFactory;
import org.graylog.storage.opensearch2.SortOrderMapper;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.junit.Rule;

public class MoreSearchAdapterOS2UsingPaginationIT extends MoreSearchAdapterIT {

    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected SearchServerInstance searchServer() {
        return openSearchInstance;
    }

    @Override
    protected MoreSearchAdapter createMoreSearchAdapter() {
        final SortOrderMapper sortOrderMapper = new SortOrderMapper();
        final OpenSearchClient client = openSearchInstance.openSearchClient();
        return new MoreSearchAdapterOS2(client, true, sortOrderMapper,
                new PaginationOS2(client,
                        new SearchRequestFactory(sortOrderMapper, false, true, new IgnoreSearchFilters())
                )

        );
    }
}
