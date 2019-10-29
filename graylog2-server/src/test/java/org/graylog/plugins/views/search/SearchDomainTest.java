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

import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.errors.EntityNotFoundException;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog2.plugin.database.users.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchDomainTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private SearchDomain sut;

    @Mock
    private SearchDbService dbService;

    @Mock
    private ViewPermissions viewPermissions;

    @Before
    public void setUp() throws Exception {
        sut = new SearchDomain(dbService, viewPermissions);
    }

    @Test
    public void throwsNotFoundWhenIdDoesntExist() {
        when(dbService.get("some-id")).thenReturn(Optional.empty());

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> sut.getForUser("some-id", mock(User.class), id -> true));
    }

    @Test
    public void loadsSearchIfUserIsOwner() {
        final User user = user("boeser-willi");

        final Search search = withOwner(user.getName());

        final Search result = sut.getForUser(search.id(), user, id -> true);

        assertThat(result).isEqualTo(search);
    }

    @Test
    public void loadsSearchIfSearchIsPermittedViaViews() {
        final User user = user("someone");
        final Search search = withOwner("someone else");

        when(viewPermissions.isSearchPermitted(eq(search.id()), eq(user), any())).thenReturn(true);

        final Search result = sut.getForUser(search.id(), user, id -> true);

        assertThat(result).isEqualTo(search);
    }

    @Test
    public void throwsPermissionExceptionIfNeitherOwnedNorPermittedFromViews() {
        final User user = user("someone");
        final Search search = withOwner("someone else");

        when(viewPermissions.isSearchPermitted(eq(search.id()), eq(user), any())).thenReturn(false);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> sut.getForUser(search.id(), user, id -> true));
    }

    private User user(String name) {
        final User user = mock(User.class);
        when(user.getName()).thenReturn(name);
        return user;
    }

    private Search withOwner(String owner) {
        Search search = Search.builder().id("some-id").owner(owner).build();
        when(dbService.get(search.id())).thenReturn(Optional.of(search));
        return search;
    }
}
