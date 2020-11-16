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
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.plugin.database.users.User;
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

    private List<Search> allSearchesInDb = new ArrayList<>();

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

        final Optional<Search> result = sut.getForUser("some-id", mock(User.class), id -> true);

        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    public void loadsSearchIfUserIsOwner() {
        final User user = user("boeser-willi");

        final Search search = mockSearchWithOwner(user.getName());

        final Optional<Search> result = sut.getForUser(search.id(), user, id -> true);

        assertThat(result).isEqualTo(Optional.of(search));
    }

    @Test
    public void loadsSearchIfSearchIsPermittedViaViews() {
        final User user = user("someone");
        final Search search = mockSearchWithOwner("someone else");


        final ViewDTO viewDTO = mock(ViewDTO.class);
        when(viewService.forSearch(anyString())).thenReturn(ImmutableList.of(viewDTO));

        final Optional<Search> result = sut.getForUser(search.id(), user, id -> true);

        assertThat(result).isEqualTo(Optional.of(search));
    }

    @Test
    public void throwsPermissionExceptionIfNeitherOwnedNorPermittedFromViews() {
        final User user = user("someone");
        final Search search = mockSearchWithOwner("someone else");

        when(viewService.forSearch(anyString())).thenReturn(ImmutableList.of());

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> sut.getForUser(search.id(), user, id -> true));
    }

    @Test
    public void includesOwnedSearchesInList() {
        final User user = user("boeser-willi");

        final Search ownedSearch = mockSearchWithOwner(user.getName());
        mockSearchWithOwner("someone else");

        List<Search> result = sut.getAllForUser(user, id -> true);

        assertThat(result).containsExactly(ownedSearch);
    }

    @Test
    public void includesSearchesPermittedViaViewsInList() {
        final User user = user("someone");

        final Search permittedSearch = mockSearchWithOwner("someone else");
        mockSearchWithOwner("someone else");

        final ViewDTO viewDTO = mock(ViewDTO.class);
        when(viewService.forSearch(anyString())).thenAnswer(invocation -> {
            if (invocation.getArgument(0).equals(permittedSearch.id())) {
                return ImmutableList.of(viewDTO);
            } else {
                return ImmutableList.of();
            }
        });

        List<Search> result = sut.getAllForUser(user, view -> true);

        assertThat(result).containsExactly(permittedSearch);
    }

    @Test
    public void listIsEmptyIfNoSearchesPermitted() {
        final User user = user("someone");

        mockSearchWithOwner("someone else");
        mockSearchWithOwner("someone else");

        List<Search> result = sut.getAllForUser(user, id -> true);

        assertThat(result).isEmpty();
    }

    private User user(String name) {
        final User user = mock(User.class);
        when(user.getName()).thenReturn(name);
        return user;
    }

    private Search mockSearchWithOwner(String owner) {
        Search search = Search.builder().id(UUID.randomUUID().toString()).owner(owner).build();
        allSearchesInDb.add(search);
        when(dbService.get(search.id())).thenReturn(Optional.of(search));
        return search;
    }
}
