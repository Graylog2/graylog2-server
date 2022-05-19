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
import org.assertj.core.api.Condition;
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
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.WARN)
public class SearchExecutorTest extends RestResourceBaseTest {
    @Mock
    private SearchDomain searchDomain;

    @Mock
    private QueryEngine queryEngine;

    @Captor
    private ArgumentCaptor<SearchJob> searchJobCaptor;

    private SearchExecutor searchExecutor;

    @BeforeEach
    void setUp() {
        final SearchJobService searchJobService = new InMemorySearchJobService();
        this.searchExecutor = new SearchExecutor(searchDomain,
                searchJobService,
                queryEngine,
                new PluggableSearchValidation(new SearchExecutionGuard(Collections.emptyMap()), Collections.emptySet()),
                new PluggableSearchNormalization(new ObjectMapperProvider().get(), Collections.emptySet()));
        when(queryEngine.execute(any(), any())).thenAnswer(invocation -> {
            final SearchJob searchJob = invocation.getArgument(0);
            searchJob.addQueryResultFuture("query", CompletableFuture.completedFuture(QueryResult.emptyResult()));
            searchJob.seal();
            return searchJob;
        });
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
        final Search search = Search.builder()
                .queries(ImmutableSet.of(Query.builder().build()))
                .build();

        final SearchUser searchUser = TestSearchUser.builder()
                .withUser(testUser -> testUser.withUsername("frank-drebin"))
                .allowStream("somestream")
                .build();

        when(searchDomain.getForUser(eq("search1"), eq(searchUser))).thenReturn(Optional.of(search));

        final SearchJob searchJob = this.searchExecutor.execute("search1", searchUser, ExecutionState.empty());
        assertThat(searchJob.getSearch().queries())
                .are(new Condition<>(query -> query.usedStreamIds().equals(Collections.singleton("somestream")), "All accessible streams have been added"));
    }

    @Test
    public void appliesSearchExecutionState() {
        final Search search = makeSearch();

        final SearchUser searchUser = TestSearchUser.builder()
                .withUser(testUser -> testUser.withUsername("frank-drebin"))
                .build();

        when(searchDomain.getForUser(eq("search1"), eq(searchUser))).thenReturn(Optional.of(search));

        final AbsoluteRange absoluteRange = AbsoluteRange.create("2022-05-18T10:00:00.000Z", "2022-05-19T10:00:00.000Z");
        final ExecutionState executionState = ExecutionState.builder()
                .setGlobalOverride(ExecutionStateGlobalOverride.builder().timerange(absoluteRange).build())
                .build();
        this.searchExecutor.execute("search1", searchUser, executionState);

        verify(queryEngine, times(1)).execute(searchJobCaptor.capture(), anySet());

        final SearchJob executedJob = searchJobCaptor.getValue();

        assertThat(executedJob.getSearch().queries())
                .are(new Condition<>(query -> query.timerange().equals(absoluteRange), "timeranges are applied through execution state"));
    }

    @Test
    public void checksUserPermissionsForSearch() {
        final Search search = Search.builder()
                .queries(ImmutableSet.of(
                        Query.builder()
                                .filter(StreamFilter.ofId("forbidden_stream"))
                                .build()
                ))
                .build();
        final SearchUser searchUser = TestSearchUser.builder()
                .denyStream("forbidden_stream")
                .build();

        when(searchDomain.getForUser(eq("search1"), eq(searchUser))).thenReturn(Optional.of(search));

        assertThatExceptionOfType(MissingStreamPermissionException.class)
                .isThrownBy(() -> this.searchExecutor.execute("search1", searchUser, ExecutionState.empty()));
    }

    private Search makeSearch() {
        return Search.builder().build();
    }
}
