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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.authorization.SearchAuthorizer;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import javax.ws.rs.ForbiddenException;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchResourceStreamPermissionsTest {
    private static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private QueryEngine queryEngine;

    @Mock
    private SearchDbService searchDbService;

    @Mock
    private SearchJobService searchJobService;

    @Mock
    private StreamService streamService;

    @Mock
    private Subject subject;

    @Mock
    private Query query;

    private SearchResource searchResource;

    static class SearchTestResource extends SearchResource {
        private final Subject subject;

        SearchTestResource(Subject subject, QueryEngine queryEngine, SearchDbService searchDbService, SearchJobService searchJobService, ObjectMapper objectMapper, StreamService streamService) {
            super(queryEngine, searchDbService, searchJobService, objectMapper, streamService, new SearchAuthorizer(Collections.emptyMap()));
            this.subject = subject;
        }

        @Override
        protected Subject getSubject() {
            return this.subject;
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            final User mockUser = mock(User.class);
            when(mockUser.getName()).thenReturn("admin");
            return mockUser;
        }
    }

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());

        this.searchResource = new SearchTestResource(subject, queryEngine, searchDbService, searchJobService, objectMapperProvider.get(), streamService);
    }

    @Test
    public void executingSearchWithoutAccessToAnyStreamsFails() {
        final String queryId = "someQuery";
        when(query.usedStreamIds()).thenReturn(ImmutableSet.of());
        when(query.toBuilder()).thenReturn(Query.builder().id(queryId).query(new BackendQuery.Fallback()).timerange(mock(RelativeRange.class)));
        when(query.id()).thenReturn(queryId);

        final String searchId = "searchId";
        final Search search = Search.Builder.create().id(searchId).queries(ImmutableSet.of(query)).build();
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.of(search));

        when(streamService.loadAll()).thenReturn(Collections.emptyList());

        thrown.expect(ForbiddenException.class);

        try {
            searchResource.executeQuery(searchId, Collections.emptyMap());
        } catch (ForbiddenException e) {
            verify(searchJobService, never()).create(any(), any());
            verify(queryEngine, never()).execute(any());

            throw e;
        }
    }

    @Test
    public void executingSyncSearchWithoutAccessToAnyStreamsFails() {
        final String queryId = "someQuery";
        when(query.usedStreamIds()).thenReturn(ImmutableSet.of());
        when(query.toBuilder()).thenReturn(Query.builder().id(queryId).query(new BackendQuery.Fallback()).timerange(mock(RelativeRange.class)));
        when(query.id()).thenReturn(queryId);

        final String searchId = "searchId";
        final Search search = Search.Builder.create().id(searchId).queries(ImmutableSet.of(query)).build();

        when(streamService.loadAll()).thenReturn(Collections.emptyList());

        thrown.expect(ForbiddenException.class);

        try {
            searchResource.executeSyncJob(search, 60000);
        } catch (ForbiddenException e) {
            verify(searchJobService, never()).create(any(), any());
            verify(queryEngine, never()).execute(any());

            throw e;
        }
    }
}
