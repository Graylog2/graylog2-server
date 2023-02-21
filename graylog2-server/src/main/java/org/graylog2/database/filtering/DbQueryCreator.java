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
package org.graylog2.database.filtering;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryParser;

import javax.ws.rs.BadRequestException;
import java.util.List;

public class DbQueryCreator {

    private final DbFilterParser dbFilterParser;
    private final SearchQueryParser searchQueryParser;
    private final List<EntityAttribute> attributes;

    public DbQueryCreator(final String defaultField,
                          final List<EntityAttribute> attributes) {
        this.dbFilterParser = new DbFilterParser();
        this.attributes = attributes;
        this.searchQueryParser = new SearchQueryParser(defaultField, attributes);
    }

    public Bson createDbQuery(final List<String> filters,
                              final String query) {
        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        List<Bson> dbFilters;
        try {
            dbFilters = dbFilterParser.parse(filters, attributes);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        return buildDbQuery(searchQuery, dbFilters);
    }

    private Bson buildDbQuery(final SearchQuery searchQuery,
                              final List<Bson> dbFilters) {
        List<Bson> filterList = searchQuery.toBsonFilterList();
        if (dbFilters != null) {
            filterList.addAll(dbFilters);
        }
        Bson dbQuery;
        if (filterList.isEmpty()) {
            dbQuery = new Document();
        } else {
            dbQuery = Filters.and(filterList);
        }
        return dbQuery;
    }
}
