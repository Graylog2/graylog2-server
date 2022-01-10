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
package org.graylog.plugins.views.search.permissions;

import org.graylog.plugins.views.search.Search;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchUserTest {
    private User mockUser(String username) {
        final User user = mock(User.class);
        when(user.getName()).thenReturn(username);
        return user;
    }

    private SearchUser searchUser(String username) {
        return new SearchUser(mockUser(username), (perm) -> true, (perm, id) -> true);
    }

    @Test
    public void exactUserOfSearchIsOwner() {
        final String username = "karl";
        final Search search = Search.builder().owner(username).build();

        final SearchUser searchUser = searchUser(username);

        assertThat(searchUser.owns(search)).isTrue();
    }

    @Test
    public void anyUserIsOwnerOfLegacySearchesWithoutOwner() {
        final String username = "karl";
        final Search search = Search.builder().build();

        final SearchUser searchUser = searchUser(username);

        assertThat(searchUser.owns(search)).isTrue();
    }

    @Test
    public void usernameNotMatchingIsNotOwner() {
        final String username = "karl";
        final Search search = Search.builder().owner("friedrich").build();

        final SearchUser searchUser = searchUser(username);

        assertThat(searchUser.owns(search)).isFalse();
    }
}
