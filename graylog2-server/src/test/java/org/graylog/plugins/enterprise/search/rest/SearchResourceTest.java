package org.graylog.plugins.enterprise.search.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.rest.SearchResource;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private StreamService streamService;

    @Mock
    private Search search;

    @Mock
    private Subject subject;

    @Mock
    private User currentUser;

    private SearchResource searchResource;

    class SearchTestResource extends SearchResource {
        private final Subject subject;

        SearchTestResource(Subject subject, QueryEngine queryEngine, SearchDbService searchDbService, SearchJobService searchJobService, ObjectMapper objectMapper, StreamService streamService) {
            super(queryEngine, searchDbService, searchJobService, objectMapper, streamService, Collections.emptyMap());
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

        this.searchResource = new SearchTestResource(subject, queryEngine, searchDbService, searchJobService, objectMapperProvider.get(), streamService);

        when(currentUser.getName()).thenReturn("admin");
    }

    @Test
    public void saveAddsOwnerToSearch() {
        final Search.Builder builder = mock(Search.Builder.class);
        when(builder.build()).thenReturn(search);
        when(builder.owner(any())).thenReturn(builder);
        when(search.toBuilder()).thenReturn(builder);

        this.searchResource.createSearch(search);

        final ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder, times(1)).owner(ownerCaptor.capture());
        assertThat(ownerCaptor.getValue()).isEqualTo("admin");
    }

    @Test
    public void getSearchAllowsAccessToSearchReturnedByService() {
        final String searchId = "deadbeef";
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.of(search));

        final Search returnedSearch = this.searchResource.getSearch(searchId);

        assertThat(returnedSearch).isNotNull();
    }

    @Test
    public void getSearchThrowsNotFoundExceptionIfNoSearchReturnedByService() {
        final String searchId = "deadbeef";
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.empty());

        try {
            this.searchResource.getSearch(searchId);

            Assert.fail();
        } catch (javax.ws.rs.NotFoundException nfe) {
            assertThat(nfe).isNotNull();
            assertThat(nfe).hasMessage("No such search deadbeef");
        }
    }

    @Test
    public void executeQueryAddsCurrentUserAsOwner() {
        final String username = "basti";
        final String searchId = "deadbeef";
        final String streamId = "streamId";
        final Query query = mock(Query.class);
        final ImmutableSet<Query> queries = ImmutableSet.of(query);
        final SearchJob searchJob = mock(SearchJob.class);

        when(query.usedStreamIds()).thenReturn(ImmutableSet.of(streamId));
        when(search.queries()).thenReturn(queries);
        when(search.applyExecutionState(any(), any())).thenReturn(search);
        when(currentUser.getName()).thenReturn(username);
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.of(search));
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":streamId")).thenReturn(true);
        when(searchJobService.create(any(), any())).thenReturn(searchJob);
        when(queryEngine.execute(any())).thenAnswer(invocation -> invocation.getArgument(0));

        this.searchResource.executeQuery(searchId, Collections.emptyMap());

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(eq(search), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo(username);
    }

    @Test
    public void executeSyncJobAddsCurrentUserAsOwner() {
        final String username = "basti";
        final SearchJob searchJob = mock(SearchJob.class);

        when(currentUser.getName()).thenReturn(username);
        when(searchJobService.create(any(), any())).thenReturn(searchJob);
        when(queryEngine.execute(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(searchJob.getResultFuture()).thenReturn(CompletableFuture.completedFuture(null));

        this.searchResource.executeSyncJob(search, 100);

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(eq(search), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo(username);
    }

    @Test
    public void executeQueryAppliesExecutionState() {
        final String username = "basti";
        final String searchId = "deadbeef";
        final String streamId = "streamId";
        final Query query = mock(Query.class);
        final ImmutableSet<Query> queries = ImmutableSet.of(query);
        final SearchJob searchJob = mock(SearchJob.class);
        final Map<String, Object> executionState = ImmutableMap.of("foo", 42);

        when(query.usedStreamIds()).thenReturn(ImmutableSet.of(streamId));
        when(search.queries()).thenReturn(queries);
        when(search.applyExecutionState(any(), any())).thenReturn(search);
        when(currentUser.getName()).thenReturn(username);
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.of(search));
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":streamId")).thenReturn(true);
        when(searchJobService.create(any(), any())).thenReturn(searchJob);
        when(queryEngine.execute(any())).thenAnswer(invocation -> invocation.getArgument(0));

        this.searchResource.executeQuery(searchId, executionState);

        final ArgumentCaptor<Map<String, Object>> executionStateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(search, times(1)).applyExecutionState(any(), executionStateCaptor.capture());

        assertThat(executionStateCaptor.getValue()).isEqualTo(executionState);
    }
}
