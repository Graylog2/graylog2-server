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
package org.graylog2.users;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserPermissionsCleanupListenerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private UserService userService;
    @Mock
    private EventBus eventBus;

    private UserPermissionsCleanupListener userPermissionsCleanupListener;

    @Before
    public void setUp() throws Exception {
        this.userPermissionsCleanupListener = new UserPermissionsCleanupListener(eventBus, userService);
    }

    @Test
    public void registerOnEventBusUponConstruction() throws Exception {
        verify(eventBus, times(1)).register(eq(userPermissionsCleanupListener));
    }

    @Test
    public void doNothingWhenListOfUsersIsEmptyForDashboardRemovals() throws Exception {
        when(userService.loadAll()).thenReturn(Collections.emptyList());

        this.userPermissionsCleanupListener.cleanupPermissionsOnDashboardRemoval(DashboardDeletedEvent.create("foobar"));

        verify(userService, never()).save(any(User.class));
    }

    @Test
    public void doNothingWhenDashboardIsNotReferenced() throws Exception {
        final User user1 = mock(User.class);
        when(user1.getPermissions()).thenReturn(Collections.emptyList());

        final User user2 = mock(User.class);
        when(user2.getPermissions()).thenReturn(ImmutableList.of(RestPermissions.DASHBOARDS_READ, RestPermissions.DASHBOARDS_EDIT));

        final List<User> users = ImmutableList.of(user1, user2);

        when(userService.loadAll()).thenReturn(users);

        this.userPermissionsCleanupListener.cleanupPermissionsOnDashboardRemoval(DashboardDeletedEvent.create("foobar"));

        verify(userService, never()).save(any(User.class));
    }

    @Test
    public void removePermissionsOfDashboardIfReferenced() throws Exception {
        final String dashboardId = "foobar";

        final User user1 = mock(User.class);
        when(user1.getPermissions()).thenReturn(ImmutableList.of(RestPermissions.DASHBOARDS_READ, RestPermissions.DASHBOARDS_EDIT));

        final User user2 = mock(User.class);
        when(user2.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.STREAMS_READ,
                RestPermissions.DASHBOARDS_READ + ":" + dashboardId,
                RestPermissions.DASHBOARDS_EDIT + ":" + dashboardId,
                RestPermissions.SEARCHES_ABSOLUTE
        ));

        final List<User> users = ImmutableList.of(user1, user2);

        when(userService.loadAll()).thenReturn(users);
        final ArgumentCaptor<List<String>> permissionsCaptor = ArgumentCaptor.forClass(List.class);

        this.userPermissionsCleanupListener.cleanupPermissionsOnDashboardRemoval(DashboardDeletedEvent.create("foobar"));

        verify(userService, never()).save(eq(user1));
        verify(userService, times(1)).save(eq(user2));
        verify(user2, times(1)).setPermissions(permissionsCaptor.capture());

        assertThat(permissionsCaptor.getValue())
                .isNotNull()
                .isNotEmpty()
                .containsExactly(
                        RestPermissions.STREAMS_READ,
                        RestPermissions.SEARCHES_ABSOLUTE
                );
    }

    @Test
    public void removePermissionsOfDashboardIfReferencedForMultipleUsers() throws Exception {
        final String dashboardId = "foobar";

        final User user1 = mock(User.class);
        when(user1.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.DASHBOARDS_READ + ":somethingelse",
                RestPermissions.DASHBOARDS_EDIT
        ));

        final User user2 = mock(User.class);
        when(user2.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.STREAMS_READ,
                RestPermissions.DASHBOARDS_READ + ":" + dashboardId,
                RestPermissions.DASHBOARDS_EDIT + ":" + dashboardId,
                RestPermissions.SEARCHES_ABSOLUTE
        ));

        final User user3 = mock(User.class);
        when(user3.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.DASHBOARDS_READ + ":" + dashboardId
        ));

        final List<User> users = ImmutableList.of(user1, user2, user3);

        when(userService.loadAll()).thenReturn(users);
        final ArgumentCaptor<List<String>> permissionsCaptorUser2 = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List<String>> permissionsCaptorUser3 = ArgumentCaptor.forClass(List.class);

        this.userPermissionsCleanupListener.cleanupPermissionsOnDashboardRemoval(DashboardDeletedEvent.create("foobar"));

        verify(userService, never()).save(eq(user1));
        verify(userService, times(1)).save(user2);
        verify(userService, times(1)).save(user3);

        verify(user2, times(1)).setPermissions(permissionsCaptorUser2.capture());
        verify(user3, times(1)).setPermissions(permissionsCaptorUser3.capture());

        assertThat(permissionsCaptorUser2.getValue())
                .isNotNull()
                .isNotEmpty()
                .containsExactly(
                        RestPermissions.STREAMS_READ,
                        RestPermissions.SEARCHES_ABSOLUTE
                );

        assertThat(permissionsCaptorUser3.getValue())
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void doNothingWhenListOfUsersIsEmptyForStreamRemovals() throws Exception {
        when(userService.loadAll()).thenReturn(Collections.emptyList());

        this.userPermissionsCleanupListener.cleanupPermissionsOnStreamRemoval(StreamDeletedEvent.create("foobar"));

        verify(userService, never()).save(any(User.class));
    }

    @Test
    public void doNothingWhenStreamIsNotReferenced() throws Exception {
        final User user1 = mock(User.class);
        when(user1.getPermissions()).thenReturn(Collections.emptyList());

        final User user2 = mock(User.class);
        when(user2.getPermissions()).thenReturn(ImmutableList.of(RestPermissions.STREAMS_READ, RestPermissions.STREAMS_EDIT));

        final List<User> users = ImmutableList.of(user1, user2);

        when(userService.loadAll()).thenReturn(users);

        this.userPermissionsCleanupListener.cleanupPermissionsOnStreamRemoval(StreamDeletedEvent.create("foobar"));

        verify(userService, never()).save(any(User.class));
    }

    @Test
    public void removePermissionsOfStreamIfReferenced() throws Exception {
        final String streamId = "foobar";

        final User user1 = mock(User.class);
        when(user1.getPermissions()).thenReturn(ImmutableList.of(RestPermissions.DASHBOARDS_READ, RestPermissions.DASHBOARDS_EDIT));

        final User user2 = mock(User.class);
        when(user2.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.DASHBOARDS_READ,
                RestPermissions.STREAMS_READ + ":" + streamId,
                RestPermissions.STREAMS_EDIT + ":" + streamId,
                RestPermissions.SEARCHES_ABSOLUTE
        ));

        final List<User> users = ImmutableList.of(user1, user2);

        when(userService.loadAll()).thenReturn(users);
        final ArgumentCaptor<List<String>> permissionsCaptor = ArgumentCaptor.forClass(List.class);

        this.userPermissionsCleanupListener.cleanupPermissionsOnStreamRemoval(StreamDeletedEvent.create("foobar"));

        verify(userService, never()).save(eq(user1));
        verify(userService, times(1)).save(eq(user2));
        verify(user2, times(1)).setPermissions(permissionsCaptor.capture());

        assertThat(permissionsCaptor.getValue())
                .isNotNull()
                .isNotEmpty()
                .containsExactly(
                        RestPermissions.DASHBOARDS_READ,
                        RestPermissions.SEARCHES_ABSOLUTE
                );
    }

    @Test
    public void removePermissionsOfStreamIfReferencedForMultipleUsers() throws Exception {
        final String streamId = "foobar";

        final User user1 = mock(User.class);
        when(user1.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.STREAMS_READ + ":somethingelse",
                RestPermissions.STREAMS_EDIT
        ));

        final User user2 = mock(User.class);
        when(user2.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.STREAMS_READ,
                RestPermissions.STREAMS_READ + ":" + streamId,
                RestPermissions.STREAMS_EDIT + ":" + streamId,
                RestPermissions.SEARCHES_ABSOLUTE
        ));

        final User user3 = mock(User.class);
        when(user3.getPermissions()).thenReturn(ImmutableList.of(
                RestPermissions.STREAMS_READ + ":" + streamId
        ));

        final List<User> users = ImmutableList.of(user1, user2, user3);

        when(userService.loadAll()).thenReturn(users);
        final ArgumentCaptor<List<String>> permissionsCaptorUser2 = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List<String>> permissionsCaptorUser3 = ArgumentCaptor.forClass(List.class);

        this.userPermissionsCleanupListener.cleanupPermissionsOnStreamRemoval(StreamDeletedEvent.create("foobar"));

        verify(userService, never()).save(eq(user1));
        verify(userService, times(1)).save(user2);
        verify(userService, times(1)).save(user3);

        verify(user2, times(1)).setPermissions(permissionsCaptorUser2.capture());
        verify(user3, times(1)).setPermissions(permissionsCaptorUser3.capture());

        assertThat(permissionsCaptorUser2.getValue())
                .isNotNull()
                .isNotEmpty()
                .containsExactly(
                        RestPermissions.STREAMS_READ,
                        RestPermissions.SEARCHES_ABSOLUTE
                );

        assertThat(permissionsCaptorUser3.getValue())
                .isNotNull()
                .isEmpty();
    }
}