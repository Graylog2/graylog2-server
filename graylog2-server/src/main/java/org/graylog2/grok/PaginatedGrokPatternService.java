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
package org.graylog2.grok;

import com.google.inject.Inject;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

public class PaginatedGrokPatternService extends PaginatedDbService<GrokPattern> {
    private static final String COLLECTION_NAME = "grok_patterns";

    @Inject
    public PaginatedGrokPatternService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider mapper)
    {
        super(mongoConnection, mapper, GrokPattern.class, COLLECTION_NAME);
    }

    public long count() {
        return db.count();
    }

    public PaginatedList<GrokPattern> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }
}
