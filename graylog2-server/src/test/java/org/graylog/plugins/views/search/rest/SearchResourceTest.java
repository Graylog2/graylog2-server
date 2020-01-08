/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.security.RestPermissions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import javax.ws.rs.ForbiddenException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private QueryEngine queryEngine;

    @Mock
    private SearchJobService searchJobService;

    @Mock
    private SearchDomain searchDomain;

    @Mock
    private Subject subject;

    @Mock
    private User currentUser;

    private SearchResource searchResource;

    class SearchTestResource extends SearchResource {
        private final Subject subject;

        SearchTestResource(Subject subject, QueryEngine queryEngine, SearchJobService searchJobService) {
            super(queryEngine, searchJobService, searchDomain);
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

        this.searchResource = new SearchTestResource(subject, queryEngine, searchJobService);
    }

    @Test
    public void getSearchLoadsSearch() {
        final Search search = mockExistingSearch();

        final Search returnedSearch = this.searchResource.getSearch(search.id());

        assertThat(returnedSearch).isEqualTo(search);
    }

    @Test
    public void ForbiddenExceptionOnAsyncExecutionIsNotCaughtSoItLeadsTo403() {
        final Search search = mockExistingSearch();

        when(searchDomain.executeAsync(any(), any(), any())).thenThrow(new ForbiddenException());

        assertThatExceptionOfType(ForbiddenException.class).isThrownBy(() -> searchResource.executeQuery(search.id(), null));
    }

    @Test
    public void ForbiddenExceptionOnSynchronousExecutionIsNotCaughtSoItLeadsTo403() {
        final Search search = mockNewSearch();

        when(searchDomain.executeSync(any(), any(), anyLong())).thenThrow(new ForbiddenException());

        assertThatExceptionOfType(ForbiddenException.class).isThrownBy(() -> searchResource.executeSyncJob(search, 1));
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
        when(searchDomain.find(eq(search.id()), any())).thenReturn(search);

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJob.getResultFuture()).thenReturn(CompletableFuture.completedFuture(null));
        when(searchJobService.create(any(), any())).thenReturn(searchJob);

        when(queryEngine.execute(any())).thenAnswer(invocation -> invocation.getArgument(0));

        return search;
    }
}
