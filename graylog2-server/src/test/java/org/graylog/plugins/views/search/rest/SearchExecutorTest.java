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
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.WARN)
public class SearchExecutorTest extends RestResourceBaseTest {

    @Mock
    private SearchDomain searchDomain;

    @Mock
    private SearchJobService searchJobService;

    @Mock
    private QueryEngine queryEngine;

    @Mock
    private SearchExecutionGuard searchExecutionGuard;

    @Captor
    private ArgumentCaptor<ExecutionState> executionStateCaptor;

    private SearchExecutor searchExecutor;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        this.searchExecutor = new SearchExecutor(searchDomain, searchJobService, queryEngine, searchExecutionGuard, objectMapper);
    }

    @Test
    public void throwsExceptionIfSearchIsNotFound() {
        final SearchUser searchUser = TestSearchUser.builder()
                .build();

        when(searchDomain.getForUser(eq("search1"), eq(searchUser))).thenReturn(Optional.empty());

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> this.searchExecutor.execute("search1", searchUser, ExecutionState.empty()))
                .withMessage("No search found with id <search1>.");
    }

    @Test
    public void addsStreamsToSearchWithoutStreams() {
        final Search search = mockSearch();

        final SearchUser searchUser = TestSearchUser.builder()
                .withUser(testUser -> testUser.withUsername("frank-drebin"))
                .build();

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJobService.create(search, "frank-drebin")).thenReturn(searchJob);
        when(searchJob.getResultFuture()).thenReturn(CompletableFuture.completedFuture(null));
        when(queryEngine.execute(searchJob)).thenReturn(searchJob);

        when(searchDomain.getForUser(eq("search1"), eq(searchUser))).thenReturn(Optional.of(search));

        this.searchExecutor.execute("search1", searchUser, ExecutionState.empty());

        verify(search, times(1)).addStreamsToQueriesWithoutStreams(any());
    }

    @Test
    public void appliesSearchExecutionState() {
        final Search search = mockSearch();

        final SearchUser searchUser = TestSearchUser.builder()
                .withUser(testUser -> testUser.withUsername("frank-drebin"))
                .build();

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJobService.create(search, "frank-drebin")).thenReturn(searchJob);
        when(searchJob.getResultFuture()).thenReturn(CompletableFuture.completedFuture(null));
        when(queryEngine.execute(searchJob)).thenReturn(searchJob);
        when(searchDomain.getForUser(eq("search1"), eq(searchUser))).thenReturn(Optional.of(search));
        final ExecutionState executionState = ExecutionState.builder().addAdditionalParameter("foo", 42).build();

        this.searchExecutor.execute("search1", searchUser, executionState);

        verify(search, times(1)).applyExecutionState(any(), executionStateCaptor.capture());

        assertThat(executionStateCaptor.getValue()).isEqualTo(executionState);
    }

    @Test
    public void checksUserPermissionsForSearch() {
        final Search search = mockSearch();
        final SearchUser searchUser = TestSearchUser.builder().build();

        doThrow(ForbiddenException.class).when(searchExecutionGuard).check(eq(search), any());
        when(searchDomain.getForUser(eq("search1"), eq(searchUser))).thenReturn(Optional.of(search));

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> this.searchExecutor.execute("search1", searchUser, ExecutionState.empty()));
    }

    private Search mockSearch() {
        final Search search = mock(Search.class);
        when(search.addStreamsToQueriesWithoutStreams(any())).thenReturn(search);
        when(search.applyExecutionState(any(), any())).thenReturn(search);
        return search;
    }
}
