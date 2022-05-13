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
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.ws.rs.NotFoundException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.WARN)
public class SearchResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SearchDomain searchDomain;

    @Mock
    private SearchJobService searchJobService;

    private final SearchUser searchUser = TestSearchUser.builder().build();

    @Mock
    private EventBus eventBus;

    @Mock
    private SearchExecutor searchExecutor;

    private SearchResource searchResource;

    @BeforeEach
    public void setUp() throws Exception {
        this.searchResource = new SearchResource(searchDomain, searchExecutor, searchJobService, eventBus);
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

        final SearchDTO returnedSearch = this.searchResource.getSearch(search.id(), searchUser);

        assertThat(returnedSearch.id()).isEqualTo(search.id());
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
        final SearchDTO search = SearchDTO.Builder.create().id(null).build();
        when(searchDomain.saveForUser(any(), any())).thenReturn(search.toSearch());

        this.searchResource.createSearch(search, searchUser);
    }

    private Search mockNewSearch() {
        final Search search = mock(Search.class);

        when(search.addStreamsToQueriesWithoutStreams(any())).thenReturn(search);

        final String streamId = "streamId";

        final Query query = mock(Query.class);
        when(query.id()).thenReturn("queryId");
        when(query.usedStreamIds()).thenReturn(ImmutableSet.of(streamId));
        when(query.searchTypes()).thenReturn(ImmutableSet.of());
        when(search.queries()).thenReturn(ImmutableSet.of(query));

        return search;
    }

    private Search mockExistingSearch() {

        final Search search = mockNewSearch();

        final String searchId = "deadbeef";
        when(search.id()).thenReturn(searchId);
        when(search.parameters()).thenReturn(ImmutableSet.of());

        when(search.applyExecutionState(any(), any())).thenReturn(search);
        when(searchDomain.getForUser(eq(search.id()), any())).thenReturn(Optional.of(search));

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJob.getResultFuture()).thenReturn(CompletableFuture.completedFuture(null));

        return search;
    }
}
