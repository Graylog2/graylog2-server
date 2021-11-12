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
import org.graylog.plugins.views.search.views.ViewService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchDomainTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SearchDomain sut;

    @Mock
    private SearchDbService dbService;

    private final List<Search> allSearchesInDb = new ArrayList<>();

    @Mock
    private ViewService viewService;

    @Before
    public void setUp() throws Exception {
        allSearchesInDb.clear();
        when(dbService.streamAll()).thenReturn(allSearchesInDb.stream());
        sut = new SearchDomain(dbService, viewService);
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
    public void listIsEmptyIfNoSearchesPermitted() {
        mockSearchWithOwner("someone else");
        mockSearchWithOwner("someone else");
        final SearchUser searchUser = mock(SearchUser.class);

        List<Search> result = sut.getAllForUser(searchUser, searchUser::canReadView);

        assertThat(result).isEmpty();
    }

    private Search mockSearchWithOwner(String owner) {
        Search search = Search.builder().id(UUID.randomUUID().toString()).owner(owner).build();
        allSearchesInDb.add(search);
        when(dbService.get(search.id())).thenReturn(Optional.of(search));
        return search;
    }
}
