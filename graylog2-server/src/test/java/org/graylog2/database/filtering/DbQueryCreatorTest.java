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
import org.bson.conversions.Bson;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.BadRequestException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DbQueryCreatorTest {

    private DbQueryCreator toTest;

    @Mock
    private DbFilterExpressionParser dbFilterParser;
    @Mock
    private SearchQueryParser searchQueryParser;
    private final List<EntityAttribute> attributes = List.of();

    @BeforeEach
    void setUp() {
        toTest = new DbQueryCreator(dbFilterParser, searchQueryParser, attributes);
    }

    @Test
    void returnsEmptyQueryOnEmptySearchQueryAndNoFilterExpressions() {
        doReturn(new SearchQuery("")).when(searchQueryParser).parse(eq(""));
        doReturn(List.of()).when(dbFilterParser).parse(List.of(), attributes);

        final Bson dbQuery = toTest.createDbQuery(List.of(), "");
        assertSame(DbQueryCreator.EMPTY_QUERY, dbQuery);
    }

    @Test
    void returnsQueryBasedOnlyOnSearchQueryIfNoFiltersFromFilterExpression() {
        final SearchQuery searchQuery = mock(SearchQuery.class);
        doReturn(List.of(Filters.eq("nvmd", "lalala"))).when(searchQuery).toBsonFilterList();
        doReturn(searchQuery).when(searchQueryParser).parse(eq("nvmd:lalala"));
        doReturn(List.of()).when(dbFilterParser).parse(List.of(), attributes);

        final Bson dbQuery = toTest.createDbQuery(List.of(), "nvmd:lalala");
        assertEquals(Filters.and(Filters.eq("nvmd", "lalala")), dbQuery);
    }

    @Test
    void returnsQueryBasedOnlyOnFilterExpressionIfNoSearchQueryProvided() {
        doReturn(new SearchQuery("")).when(searchQueryParser).parse(eq(""));
        doReturn(List.of(Filters.eq("nvmd", "lalala"), Filters.eq("hohoho", "42")))
                .when(dbFilterParser)
                .parse(List.of("nvmd:lalala", "hohoho:42"), attributes);

        final Bson dbQuery = toTest.createDbQuery(List.of("nvmd:lalala", "hohoho:42"), "");
        assertEquals(Filters.and(
                Filters.eq("nvmd", "lalala"),
                Filters.eq("hohoho", "42")
        ), dbQuery);
    }

    @Test
    void combinesSearchQueryAndFilterExpressionsToSingleQuery() {
        final SearchQuery searchQuery = mock(SearchQuery.class);
        doReturn(List.of(Filters.eq("title", "carramba"))).when(searchQuery).toBsonFilterList();
        doReturn(searchQuery).when(searchQueryParser).parse(eq("title:carramba"));
        doReturn(List.of(Filters.eq("nvmd", "lalala"), Filters.eq("hohoho", "42")))
                .when(dbFilterParser)
                .parse(List.of("nvmd:lalala", "hohoho:42"), attributes);

        final Bson dbQuery = toTest.createDbQuery(List.of("nvmd:lalala", "hohoho:42"), "title:carramba");
        assertEquals(Filters.and(
                Filters.eq("title", "carramba"),
                Filters.eq("nvmd", "lalala"),
                Filters.eq("hohoho", "42")
        ), dbQuery);
    }

    @Test
    void throwsBadRequestExceptionIfSearchQueryParserThrowsIllegalArgumentException() {
        doThrow(IllegalArgumentException.class).when(searchQueryParser).parse(eq("wrong #$%#$%$ query"));

        assertThrows(BadRequestException.class, () -> toTest.createDbQuery(List.of(), "wrong #$%#$%$ query"));
    }

    @Test
    void throwsBadRequestExceptionIfDbFilterParserThrowsIllegalArgumentException() {
        doReturn(new SearchQuery("")).when(searchQueryParser).parse(eq(""));
        doThrow(IllegalArgumentException.class).when(dbFilterParser).parse(eq(List.of("wrong #$%#$%$ filter")), eq(attributes));

        assertThrows(BadRequestException.class, () -> toTest.createDbQuery(List.of("wrong #$%#$%$ filter"), ""));
    }
}
