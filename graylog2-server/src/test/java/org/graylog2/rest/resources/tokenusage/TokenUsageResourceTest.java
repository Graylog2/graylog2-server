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
package org.graylog2.rest.resources.tokenusage;

import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsage;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.tokenusage.TokenUsageService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TokenUsageResourceTest {
    public static final int PAGE = 1;
    public static final int PER_PAGE = 10;


    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TokenUsageService tokenUsageService;

    private TokenUsageResource testee;

    @Before
    public void setUp() {
        testee = new TokenUsageResource(tokenUsageService);
    }

    @Test
    public void callingEndpointCallsService() {
        final String query = "";
        when(tokenUsageService.loadTokenUsage(eq(PAGE), eq(PER_PAGE), any(SearchQuery.class), eq(TokenUsageDTO.FIELD_NAME), eq(SortOrder.ASCENDING)))
                .thenReturn(mkPaginatedList());

        final PaginatedResponse<TokenUsage> actual = testee.getPage(PAGE, PER_PAGE, query, TokenUsageDTO.FIELD_NAME, SortOrder.ASCENDING);

        final PaginatedResponse<TokenUsage> expected = PaginatedResponse.create("token_usage", mkPaginatedList(), query);
        //Sorry. the PaginatedResponse doesn't have a proper equals():
        assertTrue(new ReflectionEquals(expected).matches(actual));

        verify(tokenUsageService, times(1)).loadTokenUsage(eq(PAGE), eq(PER_PAGE), any(SearchQuery.class), eq(TokenUsageDTO.FIELD_NAME), eq(SortOrder.ASCENDING));
    }

    private PaginatedList<TokenUsage> mkPaginatedList() {
        return PaginatedList.emptyList(PAGE, PER_PAGE);
    }
}
