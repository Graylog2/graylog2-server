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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.ForbiddenException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.graylog.plugins.views.search.TestData.searchWithQueriesWithStreams;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchDomainTest {
    private static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SearchDomain sut;

    @Mock
    private SearchDbService dbService;

    @Mock
    private QueryEngine queryEngine;

    @Mock
    private SearchJobService searchJobService;

    @Mock
    private ViewPermissions viewPermissions;

    @Mock
    private PermittedStreams permittedStreams;

    @Mock
    private SearchExecutionGuard executionGuard;

    private ObjectMapper objectMapper = objectMapperProvider.get();

    private List<Search> allSearchesInDb = new ArrayList<>();

    static class TestSearchDomain extends SearchDomain {
        public TestSearchDomain(SearchDbService dbService, QueryEngine queryEngine, SearchJobService searchJobService, ViewPermissions viewPermissions, PermittedStreams permittedStreams, SearchExecutionGuard executionGuard, ObjectMapper objectMapper) {
            super(dbService, queryEngine, searchJobService, viewPermissions, permittedStreams, executionGuard, objectMapper);
        }

        // this is a hack to facilitate testing in the short term.
        // search execution should ultimately be extracted to a dedicated class which gets stubbed here.
        @Override
        protected void forceCompletion(SearchJob runningSearchJob, long timeout) {
        }
    }

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        allSearchesInDb.clear();
        when(dbService.streamAll()).thenReturn(allSearchesInDb.stream());
        sut = new TestSearchDomain(dbService, queryEngine, searchJobService, viewPermissions, permittedStreams, executionGuard, objectMapper);
    }

    @Test
    public void throwsWhenIdDoesntExist() {
        when(dbService.get("some-id")).thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> sut.find("some-id", mock(User.class), id -> true));
    }

    @Test
    public void loadsSearchIfUserIsOwner() {
        final User user = user("boeser-willi");

        final Search search = mockExistingSearchWithOwner(user.getName());

        final Search result = sut.find(search.id(), user, id -> true);

        assertThat(result).isEqualTo(search);
    }

    @Test
    public void loadsSearchIfSearchIsPermittedViaViews() {
        final User user = user("someone");
        final Search search = mockExistingSearchWithOwner("someone else");

        when(viewPermissions.isSearchPermitted(eq(search.id()), eq(user), any())).thenReturn(true);

        final Search result = sut.find(search.id(), user, id -> true);

        assertThat(result).isEqualTo(search);
    }

    @Test
    public void throwsPermissionExceptionIfNeitherOwnedNorPermittedFromViews() {
        final User user = user("someone");
        final Search search = mockExistingSearchWithOwner("someone else");

        when(viewPermissions.isSearchPermitted(eq(search.id()), eq(user), any())).thenReturn(false);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> sut.find(search.id(), user, id -> true));
    }

    @Test
    public void includesOwnedSearchesInList() {
        final User user = user("boeser-willi");

        final Search ownedSearch = mockExistingSearchWithOwner(user.getName());
        mockExistingSearchWithOwner("someone else");

        List<Search> result = sut.getAllForUser(user, id -> true);

        assertThat(result).containsExactly(ownedSearch);
    }

    @Test
    public void includesSearchesPermittedViaViewsInList() {
        final User user = user("someone");

        final Search permittedSearch = mockExistingSearchWithOwner("someone else");
        mockExistingSearchWithOwner("someone else");

        when(viewPermissions.isSearchPermitted(eq(permittedSearch.id()), eq(user), any())).thenReturn(true);

        List<Search> result = sut.getAllForUser(user, id -> true);

        assertThat(result).containsExactly(permittedSearch);
    }

    @Test
    public void listIsEmptyIfNoSearchesPermitted() {
        final User user = user("someone");

        mockExistingSearchWithOwner("someone else");
        mockExistingSearchWithOwner("someone else");

        List<Search> result = sut.getAllForUser(user, id -> true);

        assertThat(result).isEmpty();
    }

    @Test
    public void saveAddsOwnerToSearch() {
        Search search = searchWithQueriesWithStreams().toBuilder().owner("peterchen").build();

        assignIdOnSave();

        this.sut.create(search, viewsUser("eberhard"));

        final ArgumentCaptor<Search> ownerCaptor = ArgumentCaptor.forClass(Search.class);
        verify(dbService).save(ownerCaptor.capture());

        assertThat(ownerCaptor.getValue().owner()).isEqualTo(Optional.of("eberhard"));
    }

    private void assignIdOnSave() {
        when(dbService.save(any())).thenAnswer(invocation -> ((Search) invocation.getArguments()[0]).toBuilder().id("some-id").build());
    }

    private ViewsUser viewsUser(String name) {
        return new ViewsUser(name, false, x -> true, x -> true, x -> true);
    }

    private ViewsUser viewsUser() {
        return viewsUser("peterchen");
    }

    @Test
    public void saveIsGuardedForPermissions() {
        final Search search = mockSearch();

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> sut.create(search, viewsUser()));
    }

    @Test
    public void executeAsyncAddsExecutingUserAsOwner() {
        Search search = mockExistingSearchWithOwner("peterchen");
        when(viewPermissions.isSearchPermitted(eq(search.id()), any(), any())).thenReturn(true);

        executeAsync(search, user("basti"));

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(eq(search), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo("basti");
    }

    @Test
    public void executeSyncAddsExecutingUserAsOwner() {
        final Search search = mockSearchWithOwner("peterchen");

        executeSync(search, user("basti"));

        final ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(searchJobService, times(1)).create(eq(search), usernameCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo("basti");
    }

    @Test
    public void executeAsyncAppliesExecutionState() {
        final Search search = mockExistingSearch();
        final Map<String, Object> executionState = ImmutableMap.of("foo", 42);

        when(dbService.get(search.id())).thenReturn(Optional.of(search));

        executeAsync(search, executionState, ownerOf(search));

        //noinspection unchecked
        final ArgumentCaptor<Map<String, Object>> executionStateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(search, times(1)).applyExecutionState(any(), executionStateCaptor.capture());

        assertThat(executionStateCaptor.getValue()).isEqualTo(executionState);
    }

    @Test
    public void executeAsyncIsGuardedForPermissions() {
        final Search search = mockExistingSearch();

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> executeAsync(search));
    }

    @Test
    public void executeSyncIsGuardedForPermissions() {
        final Search search = mockSearch();

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> executeSync(search));
    }

    @Test
    public void failingToAddDefaultStreamsThrowsInExecuteAsync() {
        final Search search = mockExistingSearch();

        doThrow(new ForbiddenException()).when(search).addStreamsToQueriesWithoutStreams(any());

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> executeAsync(search));
    }

    @Test
    public void failingToAddDefaultStreamsThrowsInExecuteSync() {
        final Search search = mockSearch();

        doThrow(new ForbiddenException()).when(search).addStreamsToQueriesWithoutStreams(any());

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> executeSync(search));
    }

    private void executeAsync(Search search, Map<String, Object> execState, User executingUser) {
        sut.executeAsync(search.id(), execState, x -> true, x -> true, executingUser);
    }

    private void executeAsync(Search search, User executingUser) {
        executeAsync(search, Collections.emptyMap(), executingUser);
    }

    private void executeAsync(Search search) {
        executeAsync(search, Collections.emptyMap(), ownerOf(search));
    }

    private void executeSync(Search search, User executingUser) {
        sut.executeSync(search, x -> true, executingUser, 1000);
    }

    private void executeSync(Search search) {
        executeSync(search, ownerOf(search));
    }

    private void throwGuardExceptionFor(Search search) {
        doThrow(new ForbiddenException()).when(executionGuard).check(eq(search), any());
    }

    private User user(String name) {
        final User user = mock(User.class);
        when(user.getName()).thenReturn(name);
        return user;
    }

    private User ownerOf(Search search) {
        final User user = mock(User.class);
        String name = search.owner().orElse("peterchen");
        when(user.getName()).thenReturn(name);
        return user;
    }

    private Search mockExistingSearch() {
        return mockExistingSearchWithOwner("peterchen");
    }

    private Search mockExistingSearchWithOwner(String owner) {

        Search search = mockSearchWithOwner(owner);

        allSearchesInDb.add(search);
        when(dbService.get(search.id())).thenReturn(Optional.of(search));
        return search;
    }

    private Search mockSearch() {
        return mockSearchWithOwner("peterchen");
    }

    private Search mockSearchWithOwner(String owner) {
        Search search = mock(Search.class);
        when(search.id()).thenReturn(UUID.randomUUID().toString());
        when(search.owner()).thenReturn(Optional.of(owner));
        when(search.addStreamsToQueriesWithoutStreams(any())).thenReturn(search);
        when(search.applyExecutionState(any(), any())).thenReturn(search);
        return search;
    }
}
