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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchResourceTest {
    private static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SearchExecutionGuard executionGuard;

    @Mock
    private SearchDomain searchDomain;

    @Mock
    private SearchJobService searchJobService;

    @Mock
    private SearchUser searchUser;

    @Mock
    private EventBus eventBus;

    @Mock
    private QueryEngine queryEngine;

    @Mock
    private SearchExecutor searchExecutor;

    private SearchResource searchResource;

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        this.searchResource = new SearchResource(searchDomain, searchExecutor, searchJobService, eventBus);
    }

    @Test
    public void saveAddsOwnerToSearch() {
        when(searchUser.username()).thenReturn("eberhard");
        final Search search = Search.builder().build();

        this.searchResource.createSearch(search, searchUser);

        verify(searchDomain).saveForUser(any(), eq(searchUser));
    }

    @Test
    public void testBuilderGeneratesSearchId() {
        final Search search = Search.builder().build();
        assertThat(search.id()).isNotNull();
        assertThat(org.bson.types.ObjectId.isValid(search.id())).isTrue();
    }

    @Test
    public void getSearchLoadsSearch() {
        final Search search = mockExistingSearch();

        final Search returnedSearch = this.searchResource.getSearch(search.id(), searchUser);

        assertThat(returnedSearch).isEqualTo(search);
    }

    @Test
    public void getSearchThrowsNotFoundIfSearchDoesntExist() {
        when(searchDomain.getForUser(any(), any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> this.searchResource.getSearch("god", searchUser))
                .withMessageContaining("god");
    }

    @Test
    public void allowCreatingNewSearchWithoutId() {
        final Search search = Search.builder().id(null).build();

        this.searchResource.createSearch(search, searchUser);
    }

    private Search mockNewSearch() {
        final Search search = mock(Search.class);

        when(search.addStreamsToQueriesWithoutStreams(any())).thenReturn(search);

        final String streamId = "streamId";

        final Query query = mock(Query.class);
        when(query.usedStreamIds()).thenReturn(ImmutableSet.of(streamId));
        when(search.queries()).thenReturn(ImmutableSet.of(query));

        return search;
    }

    private Search mockExistingSearch() {

        final Search search = mockNewSearch();

        final String searchId = "deadbeef";
        when(search.id()).thenReturn(searchId);

        when(search.applyExecutionState(any(), any())).thenReturn(search);
        when(searchDomain.getForUser(eq(search.id()), any())).thenReturn(Optional.of(search));

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJob.getResultFuture()).thenReturn(CompletableFuture.completedFuture(null));

        return search;
    }
}
