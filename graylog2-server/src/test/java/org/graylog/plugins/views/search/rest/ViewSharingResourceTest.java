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

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.rest.ViewSharingResource;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.sharing.UserShortSummary;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViewSharingResourceTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ViewSharingService viewSharingService;

    @Mock
    private ViewService viewService;

    @Mock
    private UserService userService;

    @Mock
    private User currentUser;

    private ViewSharingTestResource viewSharingResource;

    class ViewSharingTestResource extends ViewSharingResource {
        ViewSharingTestResource(ViewSharingService viewSharingService, ViewService viewService, UserService userService) {
            super(viewSharingService, viewService, userService);
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return currentUser;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.viewSharingResource = new ViewSharingTestResource(viewSharingService, viewService, userService);
    }

    @Test
    public void summarizeUsersReturnsListOfUsersWithoutTheCurrent() {
        when(currentUser.getName()).thenReturn("peter");
        final User user1 = mock(User.class);
        when(user1.getName()).thenReturn("franz");
        when(user1.getFullName()).thenReturn("Franz Josef Strauss");
        final User user2 = mock(User.class);
        when(user2.getName()).thenReturn("friedrich");
        when(user2.getFullName()).thenReturn("Friedrich Merz");
        final User peter = mock(User.class);
        when(peter.getName()).thenReturn("peter");

        when(userService.loadAll()).thenReturn(ImmutableList.of(user1, user2, peter));

        final Set<UserShortSummary> users = this.viewSharingResource.summarizeUsers("viewId");

        assertThat(users).containsExactlyInAnyOrder(
                UserShortSummary.create("franz", "Franz Josef Strauss"),
                UserShortSummary.create("friedrich", "Friedrich Merz")
        );
    }

    @Test
    public void summarizeUsersReturnsListOfUsersIfCurrentUserIsNull() {
        this.currentUser = null;
        final User user1 = mock(User.class);
        when(user1.getName()).thenReturn("franz");
        when(user1.getFullName()).thenReturn("Franz Josef Strauss");
        final User user2 = mock(User.class);
        when(user2.getName()).thenReturn("friedrich");
        when(user2.getFullName()).thenReturn("Friedrich Merz");
        final User peter = mock(User.class);
        when(peter.getName()).thenReturn("peter");
        when(peter.getFullName()).thenReturn("Peter Altmaier");

        when(userService.loadAll()).thenReturn(ImmutableList.of(user1, user2, peter));

        final Set<UserShortSummary> users = this.viewSharingResource.summarizeUsers("viewId");

        assertThat(users).containsExactlyInAnyOrder(
                UserShortSummary.create("franz", "Franz Josef Strauss"),
                UserShortSummary.create("friedrich", "Friedrich Merz"),
                UserShortSummary.create("peter", "Peter Altmaier")
        );
    }
}
