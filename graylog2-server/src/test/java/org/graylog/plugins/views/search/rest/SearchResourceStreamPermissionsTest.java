package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.rest.SearchResource;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private Search search;

    @Mock
    private Subject subject;

    @Mock
    private Query query;

    private SearchResource searchResource;

    static class SearchTestResource extends SearchResource {
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

    private List<Stream> allStreamsOfUser() {
        final Stream stream1 = mock(Stream.class);
        when(stream1.getId()).thenReturn("stream1id");
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":stream1id")).thenReturn(true);
        final Stream stream2 = mock(Stream.class);
        when(stream2.getId()).thenReturn("stream2id");
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":stream2id")).thenReturn(true);
        final Stream stream3 = mock(Stream.class);
        when(stream3.getId()).thenReturn("stream3id");
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":stream3id")).thenReturn(true);

        return ImmutableList.of(stream1, stream2, stream3);
    }

    @Test
    public void executingSearchWithoutStreamsUsesAllStreamsOfUser() {
        final String queryId = "someQuery";
        when(query.usedStreamIds()).thenReturn(ImmutableSet.of());
        when(query.toBuilder()).thenReturn(Query.builder().id(queryId).query(new BackendQuery.Fallback()).timerange(mock(RelativeRange.class)));
        when(query.id()).thenReturn(queryId);

        final String searchId = "searchId";
        final Search search = Search.Builder.create().id(searchId).queries(ImmutableSet.of(query)).build();
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.of(search));
        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJobService.create(any(Search.class), any(String.class))).thenReturn(searchJob);
        when(queryEngine.execute(searchJob)).thenReturn(searchJob);
        when(searchJob.getId()).thenReturn("searchJobId");

        final List<Stream> userStreams = allStreamsOfUser();
        when(streamService.loadAll()).thenReturn(userStreams);

        final Response response = searchResource.executeQuery(searchId, Collections.emptyMap());

        assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);

        final ArgumentCaptor<Search> searchCaptor = ArgumentCaptor.forClass(Search.class);
        verify(searchJobService, times(1)).create(searchCaptor.capture(), any());
        final Search modifiedSearch = searchCaptor.getValue();

        final Optional<Query> modifiedQuery = modifiedSearch.getQuery(queryId);
        assertThat(modifiedQuery).isPresent();
        assertThat(modifiedQuery.get().usedStreamIds()).containsExactlyInAnyOrder("stream1id", "stream2id", "stream3id");
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
    public void referencingNonpermittedStreamsFails() {
        final String searchId = "searchId";
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.of(search));
        when(search.queries()).thenReturn(ImmutableSet.of(query));
        when(query.usedStreamIds()).thenReturn(ImmutableSet.of("allowedstream1", "allowedstream2", "disallowedstream"));

        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":allowedstream1")).thenReturn(true);
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":allowedstream2")).thenReturn(true);
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":disallowedstream")).thenReturn(false);

        thrown.expect(ForbiddenException.class);
        thrown.expectMessage(Matchers.describedAs("Disallowed stream id must not leak.",
                Matchers.not(Matchers.containsString("disallowedstream"))));

        searchResource.executeQuery(searchId, Collections.emptyMap());
    }

    @Test
    public void referencingPermittedStreamsSucceeds() {
        final String searchId = "searchId";
        when(searchDbService.getForUser(eq(searchId), any(), any())).thenReturn(Optional.of(search));
        when(search.queries()).thenReturn(ImmutableSet.of(query));
        when(search.applyExecutionState(any(), any())).thenReturn(search);
        when(query.usedStreamIds()).thenReturn(ImmutableSet.of("allowedstream1", "allowedstream2", "allowedstream3"));

        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":allowedstream1")).thenReturn(true);
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":allowedstream2")).thenReturn(true);
        when(subject.isPermitted(RestPermissions.STREAMS_READ + ":allowedstream3")).thenReturn(true);

        final SearchJob searchJob = mock(SearchJob.class);
        when(searchJobService.create(eq(search), any(String.class))).thenReturn(searchJob);
        when(queryEngine.execute(searchJob)).thenReturn(searchJob);
        when(searchJob.getId()).thenReturn("searchJobId");

        final Response response = searchResource.executeQuery(searchId, Collections.emptyMap());

        assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);

        final ArgumentCaptor<String> permissionCaptor = ArgumentCaptor.forClass(String.class);
        verify(subject, times(3)).isPermitted(permissionCaptor.capture());
        assertThat(permissionCaptor.getAllValues()).containsExactlyInAnyOrder(
                RestPermissions.STREAMS_READ + ":allowedstream1",
                RestPermissions.STREAMS_READ + ":allowedstream2",
                RestPermissions.STREAMS_READ + ":allowedstream3"
        );
    }
}
