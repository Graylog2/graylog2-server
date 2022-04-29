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
package org.graylog.plugins.views.search;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchDomainTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SearchDomain sut;

    @Mock
    private SearchDbService dbService;

    @Mock
    private SearchExecutionGuard executionGuard;

    private final List<Search> allSearchesInDb = new ArrayList<>();

    @Mock
    private ViewService viewService;

    @Before
    public void setUp() throws Exception {
        allSearchesInDb.clear();
        when(dbService.streamAll()).thenReturn(allSearchesInDb.stream());
        sut = new SearchDomain(dbService, executionGuard, viewService, new HashMap<>());
    }

    @Test
    public void returnsEmptyOptionalWhenIdDoesntExist() {
        when(dbService.get("some-id")).thenReturn(Optional.empty());
        final SearchUser searchUser = mock(SearchUser.class);

        final Optional<Search> result = sut.getForUser("some-id", searchUser);

        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    public void loadsSearchIfUserIsOwner() {
        final String userName = "boeser-willi";

        final Search search = mockSearchWithOwner(userName);
        final SearchUser searchUser = mock(SearchUser.class);
        when(searchUser.owns(search)).thenReturn(true);

        final Optional<Search> result = sut.getForUser(search.id(), searchUser);

        assertThat(result).isEqualTo(Optional.of(search));
    }

    @Test
    public void loadsSearchIfSearchIsPermittedViaViews() {
        final Search search = mockSearchWithOwner("someone else");
        final SearchUser searchUser = mock(SearchUser.class);

        final ViewDTO viewDTO = mock(ViewDTO.class);
        when(viewService.forSearch(anyString())).thenReturn(ImmutableList.of(viewDTO));
        when(searchUser.canReadView(viewDTO)).thenReturn(true);

        final Optional<Search> result = sut.getForUser(search.id(), searchUser);

        assertThat(result).isEqualTo(Optional.of(search));
    }

    @Test
    public void throwsPermissionExceptionIfNeitherOwnedNorPermittedFromViews() {
        final Search search = mockSearchWithOwner("someone else");
        final SearchUser searchUser = mock(SearchUser.class);

        when(viewService.forSearch(anyString())).thenReturn(ImmutableList.of());

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> sut.getForUser(search.id(), searchUser));
    }

    @Test
    public void includesOwnedSearchesInList() {
        final String userName = "boeser-willi";

        final Search ownedSearch = mockSearchWithOwner(userName);
        mockSearchWithOwner("someone else");
        final SearchUser searchUser = mock(SearchUser.class);
        when(searchUser.owns(ownedSearch)).thenReturn(true);

        List<Search> result = sut.getAllForUser(searchUser, searchUser::canReadView);

        assertThat(result).containsExactly(ownedSearch);
    }

    @Test
    public void includesSearchesPermittedViaViewsInList() {
        final Search permittedSearch = mockSearchWithOwner("someone else");
        mockSearchWithOwner("someone else");
        final SearchUser searchUser = mock(SearchUser.class);

        final ViewDTO viewDTO = mock(ViewDTO.class);
        when(viewService.forSearch(permittedSearch.id())).thenReturn(ImmutableList.of(viewDTO));
        when(searchUser.canReadView(viewDTO)).thenReturn(true);

        List<Search> result = sut.getAllForUser(searchUser, searchUser::canReadView);

        assertThat(result).containsExactly(permittedSearch);
    }

    @Test
    public void includesSearchesPermittedViaResolvedView() {
        Search permittedSearch = Search.builder().id(UUID.randomUUID().toString()).owner("someone else").build();
        allSearchesInDb.add(permittedSearch);
        when(dbService.get(permittedSearch.id())).thenReturn(Optional.of(permittedSearch));
        final SearchUser searchUser = mock(SearchUser.class);
        final ViewDTO viewDTO = mock(ViewDTO.class);
        when(searchUser.canReadView(viewDTO)).thenReturn(true);

        // Prepare test ViewResolver that returns a view that should be permitted.
        final SearchDomain searchDomain = new SearchDomain(dbService, executionGuard, viewService,
                testViewResolvers(viewDTO));

        List<Search> result = searchDomain.getAllForUser(searchUser, searchUser::canReadView);
        assertThat(result).containsExactly(permittedSearch);
    }

    @Test
    public void listIsEmptyIfNoSearchesPermitted() {
        mockSearchWithOwner("someone else");
        mockSearchWithOwner("someone else");
        final SearchUser searchUser = mock(SearchUser.class);

        List<Search> result = sut.getAllForUser(searchUser, searchUser::canReadView);

        assertThat(result).isEmpty();
    }

    @Test
    public void guardExceptionOnPostLeadsTo403() {
        final Search search = mockSearchWithOwner("someone");
        final SearchUser searchUser = mock(SearchUser.class);

        throwGuardExceptionFor(search);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> sut.saveForUser(search, searchUser));
    }

    @Test
    public void saveAddsOwnerToSearch() {
        final Search search = mockSearchWithOwner(null);
        final SearchUser searchUser = mock(SearchUser.class);
        when(searchUser.username()).thenReturn("eberhard");
        when(searchUser.isAdmin()).thenReturn(true);

        sut.saveForUser(search, searchUser);

        final ArgumentCaptor<Search> savedCaptor = ArgumentCaptor.forClass(Search.class);
        verify(dbService, times(1)).save(savedCaptor.capture());

        final Search result = savedCaptor.getValue();

        assertThat(result.owner()).contains("eberhard");
    }

    private void throwGuardExceptionFor(Search search) {
        doThrow(new ForbiddenException()).when(executionGuard).check(eq(search), any());
    }

    private Search mockSearchWithOwner(String owner) {
        Search search = Search.builder().id(UUID.randomUUID().toString()).owner(owner).build();
        allSearchesInDb.add(search);
        when(dbService.get(search.id())).thenReturn(Optional.of(search));
        return search;
    }

    private HashMap<String, ViewResolver> testViewResolvers(ViewDTO viewDTO) {
        final HashMap<String, ViewResolver> viewResolvers = new HashMap<>();
        viewResolvers.put("test-resolver", new TestViewResolver(viewDTO));
        return viewResolvers;
    }

    private static class TestViewResolver implements ViewResolver {

        private final ViewDTO viewDTO;

        public TestViewResolver(ViewDTO viewDTO) {
            this.viewDTO = viewDTO;
        }

        @Override
        public Optional<ViewDTO> get(String id) {
            return Optional.empty();
        }

        @Override
        public Set<String> getSearchIds() {
            return Collections.emptySet();
        }

        @Override
        public boolean canReadView(String viewId, Predicate<String> permissionTester, BiPredicate<String, String> entityPermissionsTester) {
            return false;
        }

        @Override
        public Set<ViewDTO> getBySearchId(String searchId) {
            return Collections.singleton(viewDTO);
        }
    }
}
