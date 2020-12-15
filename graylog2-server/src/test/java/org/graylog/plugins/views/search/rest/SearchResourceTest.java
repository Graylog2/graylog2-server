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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.events.SearchJobExecutionEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.RestPermissions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
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

public class SearchResourceTest {
    private static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private QueryEngine queryEngine;

    @Mock
    private SearchDbService searchDbService;

    @Mock
    private SearchJobService searchJobService;

    @Mock
    private PermittedStreams permittedStreams;

    @Mock
    private SearchExecutionGuard executionGuard;

    @Mock
    private SearchDomain searchDomain;

    @Mock
    private Subject subject;

    @Mock
    private User currentUser;

    @Mock
    private EventBus eventBus;

    private SearchResource searchResource;

    class SearchTestResource extends SearchResource {
        private final Subject subject;

        SearchTestResource(Subject subject, QueryEngine queryEngine, SearchDbService searchDbService, SearchJobService searchJobService, ObjectMapper objectMapper, PermittedStreams streamLoader) {
            super(queryEngine, searchDbService, searchJobService, objectMapper, streamLoader, executionGuard, searchDomain, eventBus);
            this.subject = subject;
        }

        @Override
        protected Subject getSubject() {
            return this.subject;
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return currentUser;
        }
    }

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());

        this.searchResource = new SearchTestResource(subject, queryEngine, searchDbService, searchJobService, objectMapperProvider.get(), permittedStreams);

        when(currentUser.getName()).thenReturn("admin");
    }

    @Test
    public void saveAddsOwnerToSearch() {
        when(currentUser.getName()).thenReturn("eberhard");
        final Search search = Search.builder().build();

        this.searchResource.createSearch(search);

        final ArgumentCaptor<Search> ownerCaptor = ArgumentCaptor.forClass(Search.class);
        verify(searchDbService).save(ownerCaptor.capture());

        assertThat(ownerCaptor.getValue().owner()).isEqualTo(Optional.of("eberhard"));
    }

    @Test
    public void getSearchLoadsSearch() {
        final Search search = mockExistingSearch();

        final Search returnedSearch = this.searchResource.getSearch(search.id());

        assertThat(returnedSearch).isEqualTo(search);
    }

    @Test
    public void getSearchThrowsNotFoundIfSearchDoesntExist() {
        when(searchDomain.getForUser(any(), any(), any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> this.searchResource.getSearch("god"))
                .withMessageContaining("god");
    }

    @Test
    public void executeQueryAddsCurrentUserAsOwner() {
        mockCurrentUserName("basti");

        final Search search = mockExistingSearch();

        this.searchResource.executeQuery(search.id(), Collections.emptyMap());

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(eq(search), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo("basti");
    }

    @Test
    public void executeQueryTriggersEvent() {
        mockCurrentUserName("basti");

        final Search search = mockExistingSearch();

        final Response response = this.searchResource.executeQuery(search.id(), Collections.emptyMap());

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
            this.searchResource.executeQuery(search.id(), Collections.emptyMap());
        } catch (ForbiddenException ignored) {}

        verify(this.eventBus, never()).post(any(SearchJobExecutionEvent.class));
    }

    @Test
    public void executeSyncJobTriggersEvent() {
        mockCurrentUserName("peterchen");

        final Search search = mockExistingSearch();

        final Response response = this.searchResource.executeSyncJob(search, 100);

        final ArgumentCaptor<SearchJobExecutionEvent> eventCaptor = ArgumentCaptor.forClass(SearchJobExecutionEvent.class);
        verify(this.eventBus, times(1)).post(eventCaptor.capture());

        final SearchJobExecutionEvent searchJobExecutionEvent = eventCaptor.getValue();
        assertThat(searchJobExecutionEvent.user()).isEqualTo(currentUser);
        assertThat(searchJobExecutionEvent.searchJob()).isEqualTo(response.getEntity());
    }

    @Test
    public void guardExceptionDuringSyncExecutionPreventsTriggeringEvent() {
        mockCurrentUserName("basti");

        final Search search = mockExistingSearch();
        throwGuardExceptionFor(search);

        try {
            this.searchResource.executeSyncJob(search, 100);
        } catch (ForbiddenException ignored) {}

        verify(this.eventBus, never()).post(any(SearchJobExecutionEvent.class));
    }

    @Test
    public void executeSyncJobAddsCurrentUserAsOwner() {
        mockCurrentUserName("peterchen");

        final Search search = mockExistingSearch();

        this.searchResource.executeSyncJob(search, 100);

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(eq(search), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo("peterchen");
    }

    @Test
    public void executeQueryAppliesExecutionState() {
        final Search search = mockExistingSearch();
        final Map<String, Object> executionState = ImmutableMap.of("foo", 42);

        when(searchDbService.get(search.id())).thenReturn(Optional.of(search));

        this.searchResource.executeQuery(search.id(), executionState);

        //noinspection unchecked
        final ArgumentCaptor<Map<String, Object>> executionStateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(search, times(1)).applyExecutionState(any(), executionStateCaptor.capture());

        assertThat(executionStateCaptor.getValue()).isEqualTo(executionState);
    }

    @Test
    public void guardExceptionInAsyncExecutionLeadsTo403() {
        final Search search = mockExistingSearch();

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeQuery(search.id(), null));
    }

    @Test
    public void guardExceptionInSynchronousExecutionLeadsTo403() {
        final Search search = mockExistingSearch();

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeSyncJob(search, 0));
    }

    @Test
    public void failureToAddDefaultStreamsInAsyncSearchLeadsTo403() {
        final Search search = mockExistingSearch();

        doThrow(new ForbiddenException()).when(search).addStreamsToQueriesWithoutStreams(any());

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeQuery(search.id(), null));
    }

    @Test
    public void failureToAddDefaultStreamsInSynchronousSearchLeadsTo403() {
        final Search search = mockExistingSearch();

        doThrow(new ForbiddenException()).when(search).addStreamsToQueriesWithoutStreams(any());

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchResource.executeSyncJob(search, 0));
    }

    @Test
    public void guardExceptionOnPostLeadsTo403() {
        final Search search = mockNewSearch();

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> searchResource.createSearch(search));
    }

    private void mockCurrentUserName(String name) {
        when(currentUser.getName()).thenReturn(name);
    }

    private void throwGuardExceptionFor(Search search) {
        doThrow(new ForbiddenException()).when(executionGuard).check(eq(search), any());
    }

    private Search mockNewSearch() {
        final Search search = mock(Search.class);

        when(search.addStreamsToQueriesWithoutStreams(any())).thenReturn(search);

        final String streamId = "streamId";
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":" + streamId)).thenReturn(true);

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
        when(searchDomain.getForUser(eq(search.id()), any(), any())).thenReturn(Optional.of(search));

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJob.getResultFuture()).thenReturn(CompletableFuture.completedFuture(null));
        when(searchJobService.create(any(), any())).thenReturn(searchJob);

        when(queryEngine.execute(any())).thenAnswer(invocation -> invocation.getArgument(0));

        return search;
    }
}
