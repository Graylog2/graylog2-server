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
package org.graylog2.utilities.lucene;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;

import java.io.IOException;

public interface InMemorySearchEngine<T extends  InMemorySearchableEntity> {
    PaginatedList<T> search(SearchQuery queryString, String sortField, SortOrder sortOrder, int page, int perPage) throws IOException, QueryNodeException;
}
