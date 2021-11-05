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
