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
import org.graylog.plugins.views.search.db.InMemorySearchJobService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.engine.normalization.PluggableSearchNormalization;
import org.graylog.plugins.views.search.engine.validation.PluggableSearchValidation;
import org.graylog.plugins.views.search.events.SearchJobExecutionEvent;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.WARN)
public class SearchResourceExecutionTest {
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SearchUser searchUser;

    private SearchResource searchResource;

    private SearchJobService searchJobService;

    @BeforeEach
    public void setUp() {
        this.searchJobService = new InMemorySearchJobService();
        final SearchExecutor searchExecutor = new SearchExecutor(searchDomain,
                searchJobService,
                queryEngine,
                new PluggableSearchValidation(executionGuard, Collections.emptySet()),
                new PluggableSearchNormalization(new ObjectMapperProvider().get(), Collections.emptySet()));

        this.searchResource = new SearchResource(searchDomain, searchExecutor, searchJobService, eventBus) {
            @Override
            protected User getCurrentUser() {
                return currentUser;
            }
        };
    }

    @Test
    public void executeQueryAddsCurrentUserAsOwner() {
        final String username = "basti";
        mockCurrentUserName(username);

        final Search search = makeExistingSearch(username);

        final Response response = this.searchResource.executeQuery(search.id(), ExecutionState.empty(), searchUser);

        final SearchJob searchJob = (SearchJob)response.getEntity();

        assertThat(searchJob.getOwner()).isEqualTo(username);
    }

    @Test
    public void executeQueryTriggersEvent() {
        final String username = "basti";
        mockCurrentUserName(username);

        final Search search = makeExistingSearch(username);

        final Response response = this.searchResource.executeQuery(search.id(), ExecutionState.empty(), searchUser);

        final ArgumentCaptor<SearchJobExecutionEvent> eventCaptor = ArgumentCaptor.forClass(SearchJobExecutionEvent.class);
        verify(this.eventBus, times(1)).post(eventCaptor.capture());

        final SearchJobExecutionEvent searchJobExecutionEvent = eventCaptor.getValue();
        assertThat(searchJobExecutionEvent.user()).isEqualTo(currentUser);
        assertThat(searchJobExecutionEvent.searchJob()).isEqualTo(response.getEntity());
    }

    @Test
    public void guardExceptionPreventsTriggeringEvent() {
        final String username = "basti";
        mockCurrentUserName(username);

        final Search search = makeExistingSearch(username);
        throwGuardExceptionFor();

        try {
            this.searchResource.executeQuery(search.id(), ExecutionState.empty(), searchUser);
        } catch (ForbiddenException ignored) {
        }

        verify(this.eventBus, never()).post(any(SearchJobExecutionEvent.class));
    }

    @Test
    public void executeSyncJobTriggersEvent() {
        mockCurrentUserName("peterchen");

        final SearchDTO search = makeSearchDTO();

        final SearchJob searchJob = new SearchJob("deadbeef", search.toSearch(), "peterchen");
        searchJob.addQueryResultFuture("query", CompletableFuture.completedFuture(QueryResult.emptyResult()));
        searchJob.seal();

        when(queryEngine.execute(any(), any())).thenReturn(searchJob);

        final Response response = this.searchResource.executeSyncJob(search, 100, searchUser);

        final ArgumentCaptor<SearchJobExecutionEvent> eventCaptor = ArgumentCaptor.forClass(SearchJobExecutionEvent.class);
        verify(this.eventBus, times(1)).post(eventCaptor.capture());

        final SearchJobExecutionEvent searchJobExecutionEvent = eventCaptor.getValue();
        assertThat(searchJobExecutionEvent)
                .extracting(SearchJobExecutionEvent::user)
                .containsExactly(currentUser);
        assertThat(searchJobExecutionEvent)
                .extracting(event -> event.searchJob().getId())
                .containsExactly("deadbeef");
    }

    @Test
    public void guardExceptionDuringSyncExecutionPreventsTriggeringEvent() {
        mockCurrentUserName("basti");

        final SearchDTO search = makeSearchDTO();
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

        final SearchDTO search = makeSearchDTO();

        final SearchJob searchJob = makeSearchJob(search.toSearch());

        when(queryEngine.execute(any(), any())).thenReturn(searchJob);

        final Response response = this.searchResource.executeSyncJob(search, 100, searchUser);

        final SearchJobDTO responseSearchJob = (SearchJobDTO)response.getEntity();
        assertThat(responseSearchJob.owner()).isEqualTo("peterchen");
    }

    @Test
    public void guardExceptionInAsyncExecutionLeadsTo403() {
        final Search search = makeExistingSearch("basti");

        throwGuardExceptionFor();

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeQuery(search.id(), null, searchUser));
    }

    @Test
    public void guardExceptionInSynchronousExecutionLeadsTo403() {
        final SearchDTO search = makeSearchDTO();

        throwGuardException();

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeSyncJob(search, 0, searchUser));
    }

    @Test
    public void failureToAddDefaultStreamsInAsyncSearchLeadsTo403() {
        final Search search = makeNewSearch("search1");
        persistSearch(search);

        when(searchUser.streams().loadAll()).thenReturn(ImmutableSet.of());

        assertThatExceptionOfType(MissingStreamPermissionException.class)
                .isThrownBy(() -> this.searchResource.executeQuery(search.id(), null, searchUser));
    }

    @Test
    public void failureToAddDefaultStreamsInSynchronousSearchLeadsTo403() {
        final Search search = makeNewSearch("search1");
        final SearchDTO searchDTO = SearchDTO.fromSearch(search);

        when(searchUser.streams().loadAll()).thenReturn(ImmutableSet.of());

        assertThatExceptionOfType(MissingStreamPermissionException.class)
                .isThrownBy(() -> this.searchResource.executeSyncJob(searchDTO, 0, searchUser));
    }

    private void throwGuardExceptionFor() {
        doThrow(new ForbiddenException()).when(executionGuard).check(any(), any());
    }

    private void throwGuardException() {
        doThrow(new ForbiddenException()).when(executionGuard).check(any(), any());
    }

    private void mockCurrentUserName(String name) {
        when(currentUser.getName()).thenReturn(name);
        when(searchUser.username()).thenReturn(name);
    }

    private Search makeNewSearch(String searchId) {
        final String streamId = "streamId";
        final Query query1 = Query.builder()
                .id("query1")
                .filter(StreamFilter.ofId(streamId))
                .build();
        final Query query2 = Query.builder()
                .id("query2")
                .build();
        return Search.builder()
                .id(searchId)
                .queries(ImmutableSet.of(query1, query2))
                .build();
    }

    private Search makeExistingSearch(String owner) {
        final Search search = makeNewSearch("deadbeef")
                .toBuilder()
                .owner(owner)
                .build();

        persistSearch(search);

        when(queryEngine.execute(any(), any())).thenAnswer(invocation -> {
            final SearchJob searchJob = invocation.getArgument(0);
            searchJob.addQueryResultFuture("query", CompletableFuture.completedFuture(QueryResult.emptyResult()));
            searchJob.seal();
            return searchJob;
        });

        return search;
    }

    private void persistSearch(Search search) {
        when(searchDomain.getForUser(eq(search.id()), any())).thenReturn(Optional.of(search));
    }

    private SearchDTO makeSearchDTO() {
        return SearchDTO.Builder
                .create()
                .build();
    }

    private SearchJob makeSearchJob(Search search) {
        final SearchJob searchJob = new SearchJob("deadbeef", search, "peterchen");
        searchJob.addQueryResultFuture("query1", CompletableFuture.completedFuture(QueryResult.emptyResult()));
        searchJob.seal();

        return searchJob;
    }
}
