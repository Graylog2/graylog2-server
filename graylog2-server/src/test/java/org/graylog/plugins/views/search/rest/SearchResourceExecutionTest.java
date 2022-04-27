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
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.events.SearchJobExecutionEvent;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchResourceExecutionTest {
    private static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SearchJobService searchJobService;

    @Mock
    private EventBus eventBus;

    @Mock
    private QueryEngine queryEngine;

    @Mock
    private SearchExecutionGuard executionGuard;

    @Mock
    private SearchDomain searchDomain;

    @Mock
    private User currentUser;

    @Mock
    private SearchUser searchUser;

    private SearchResource searchResource;

    @Before
    public void setUp() {
        final SearchExecutor searchExecutor = new SearchExecutor(searchDomain,
                searchJobService,
                queryEngine,
                executionGuard,
                objectMapperProvider.get());

        this.searchResource = new SearchResource(searchDomain, searchExecutor, searchJobService, eventBus) {
            @Override
            protected User getCurrentUser() {
                return currentUser;
            }
        };
    }

    @Test
    public void executeQueryAddsCurrentUserAsOwner() {
        mockCurrentUserName("basti");

        final Search search = mockExistingSearch();

        this.searchResource.executeQuery(search.id(), ExecutionState.empty(), searchUser);

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(eq(search), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo("basti");
    }

    @Test
    public void executeQueryTriggersEvent() {
        mockCurrentUserName("basti");

        final Search search = mockExistingSearch();

        final Response response = this.searchResource.executeQuery(search.id(), ExecutionState.empty(), searchUser);

        final ArgumentCaptor<SearchJobExecutionEvent> eventCaptor = ArgumentCaptor.forClass(SearchJobExecutionEvent.class);
        verify(this.eventBus, times(1)).post(eventCaptor.capture());

        final SearchJobExecutionEvent searchJobExecutionEvent = eventCaptor.getValue();
        assertThat(searchJobExecutionEvent.user()).isEqualTo(currentUser);
        assertThat(searchJobExecutionEvent.searchJob()).isEqualTo(response.getEntity());
    }

    @Test
    public void guardExceptionPreventsTriggeringEvent() {
        mockCurrentUserName("basti");

        final Search search = mockExistingSearch();
        throwGuardExceptionFor(search);

        try {
            this.searchResource.executeQuery(search.id(), ExecutionState.empty(), searchUser);
        } catch (ForbiddenException ignored) {
        }

        verify(this.eventBus, never()).post(any(SearchJobExecutionEvent.class));
    }

    @Test
    public void executeSyncJobTriggersEvent() {
        mockCurrentUserName("peterchen");

        final SearchDTO search = mockSearchDTO();

        final SearchJob searchJob = new SearchJob("deadbeef", search.toSearch(), "peterchen");
        searchJob.addQueryResultFuture("query", CompletableFuture.completedFuture(QueryResult.emptyResult()));
        searchJob.seal();

        when(queryEngine.execute(any())).thenReturn(searchJob);

        final Response response = this.searchResource.executeSyncJob(search, 100, searchUser);

        final ArgumentCaptor<SearchJobExecutionEvent> eventCaptor = ArgumentCaptor.forClass(SearchJobExecutionEvent.class);
        verify(this.eventBus, times(1)).post(eventCaptor.capture());

        final SearchJobExecutionEvent searchJobExecutionEvent = eventCaptor.getValue();
        assertThat(searchJobExecutionEvent.user()).isEqualTo(currentUser);
        assertThat(searchJobExecutionEvent.searchJob().getId()).isEqualTo("deadbeef");
    }

    @Test
    public void guardExceptionDuringSyncExecutionPreventsTriggeringEvent() {
        mockCurrentUserName("basti");

        final SearchDTO search = mockSearchDTO();
        doThrow(new ForbiddenException()).when(executionGuard).check(any(), any());

        try {
            this.searchResource.executeSyncJob(search, 100, searchUser);
        } catch (ForbiddenException ignored) {
        }

        verify(this.eventBus, never()).post(any(SearchJobExecutionEvent.class));
    }

    @Test
    public void executeSyncJobAddsCurrentUserAsOwner() {
        mockCurrentUserName("peterchen");

        final SearchDTO search = mockSearchDTO();

        final SearchJob searchJob = mocKSearchJob(search.toSearch());

        when(queryEngine.execute(any())).thenReturn(searchJob);

        this.searchResource.executeSyncJob(search, 100, searchUser);

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(any(), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo("peterchen");
    }

    @Test
    public void executeQueryAppliesExecutionState() {
        final Search search = mockExistingSearch();

        final ExecutionState.Builder builder = ExecutionState.builder();
        builder.addAdditionalParameter("foo", 42);

        final ExecutionState executionState = builder.build();
        this.searchResource.executeQuery(search.id(), executionState, searchUser);

        final ArgumentCaptor<ExecutionState> executionStateCaptor = ArgumentCaptor.forClass(ExecutionState.class);
        verify(search, times(1)).applyExecutionState(any(), executionStateCaptor.capture());

        assertThat(executionStateCaptor.getValue()).isEqualTo(executionState);
    }

    @Test
    public void guardExceptionInAsyncExecutionLeadsTo403() {
        final Search search = mockExistingSearch();

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeQuery(search.id(), null, searchUser));
    }

    @Test
    public void guardExceptionInSynchronousExecutionLeadsTo403() {
        final SearchDTO search = mockSearchDTO();

        throwGuardException();

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeSyncJob(search, 0, searchUser));
    }

    @Test
    public void failureToAddDefaultStreamsInAsyncSearchLeadsTo403() {
        final Search search = mockExistingSearch();

        doThrow(new ForbiddenException()).when(search).addStreamsToQueriesWithoutStreams(any());

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeQuery(search.id(), null, searchUser));
    }

    @Test
    public void failureToAddDefaultStreamsInSynchronousSearchLeadsTo403() {
        final SearchDTO searchDTO = mock(SearchDTO.class);
        final Search search = mockExistingSearch();
        when(searchDTO.toSearch()).thenReturn(search);

        doThrow(new ForbiddenException()).when(search).addStreamsToQueriesWithoutStreams(any());

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeSyncJob(searchDTO, 0, searchUser));
    }

    private void throwGuardExceptionFor(Search search) {
        doThrow(new ForbiddenException()).when(executionGuard).check(eq(search), any());
    }

    private void throwGuardException() {
        doThrow(new ForbiddenException()).when(executionGuard).check(any(), any());
    }

    private void mockCurrentUserName(String name) {
        when(currentUser.getName()).thenReturn(name);
        when(searchUser.username()).thenReturn(name);
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

        final SearchJob searchJob = mocKSearchJob(search);
        when(searchJobService.create(any(), any())).thenReturn(searchJob);

        when(queryEngine.execute(any())).thenAnswer(invocation -> invocation.getArgument(0));

        return search;
    }

    private SearchDTO mockSearchDTO() {
        return SearchDTO.Builder
                .create()
                .queries(ImmutableSet.of())
                .build();
    }

    private SearchJob mocKSearchJob(Search search) {
        final SearchJob searchJob = new SearchJob("deadbeef", search, "peterchen");
        searchJob.addQueryResultFuture("query1", CompletableFuture.completedFuture(QueryResult.emptyResult()));
        searchJob.seal();

        return searchJob;
    }
}
