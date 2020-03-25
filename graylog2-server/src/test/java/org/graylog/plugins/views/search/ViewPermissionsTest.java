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

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharing;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.plugin.database.users.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.views.ViewDTO.idsFrom;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViewPermissionsTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ViewPermissions sut;

    @Mock
    private IsViewSharedForUser isViewSharedForUser;

    @Mock
    private ViewSharingService viewSharingService;

    @Mock
    private ViewService viewService;

    @Before
    public void setUp() throws Exception {
        sut = new ViewPermissions(viewService, viewSharingService, isViewSharedForUser);
    }

    @Test
    public void deniesIfNoViewsForSearch() {
        boolean result = sut.isSearchPermitted("some-id", mock(User.class), id -> true);

        assertThat(result).isFalse();
    }

    @Test
    public void allowsIfThereIsASharedViewForUserWithSearch() {
        User user = mock(User.class);

        mockViewWithSharingStatusForUser(user, true);

        boolean result = sut.isSearchPermitted("some-id", user, id -> false);

        assertThat(result).isTrue();
    }

    @Test
    public void deniesIfViewIsNotSharedWithAnybody() {
        ViewDTO sharedView = someView();
        User user = mock(User.class);

        ImmutableSet<ViewDTO> viewDTOS = mockSomeViewsIncluding(sharedView);

        when(viewSharingService.forViews(idsFrom(viewDTOS))).thenReturn(ImmutableSet.of());

        boolean result = sut.isSearchPermitted("some-id", user, id -> false);

        assertThat(result).isFalse();
    }

    @Test
    public void deniesIfViewIsSharedButNotWithUser() {
        User user = mock(User.class);

        mockViewWithSharingStatusForUser(user, false);

        boolean result = sut.isSearchPermitted("some-id", user, id -> false);

        assertThat(result).isFalse();
    }

    @Test
    public void allowsIfUserHasDirectReadPermissionForViewWithSearch() {
        ViewDTO permittedView = someView();

        mockSomeViewsIncluding(permittedView);

        boolean result = sut.isSearchPermitted("some-id", mock(User.class), view -> view.id().equals(permittedView.id()));

        assertThat(result).isTrue();
    }

    private void mockViewWithSharingStatusForUser(User user, boolean isSharedWithUser) {
        ViewDTO sharedView = someView();
        Set<ViewDTO> views = mockSomeViewsIncluding(sharedView);
        Set<String> viewIds = idsFrom(views);
        ViewSharing viewSharing = mock(ViewSharing.class);
        when(viewSharingService.forViews(viewIds)).thenReturn(ImmutableSet.of(viewSharing));
        when(isViewSharedForUser.isAllowedToSee(user, viewSharing)).thenReturn(isSharedWithUser);
    }

    private ImmutableSet<ViewDTO> mockSomeViewsIncluding(ViewDTO sharedView) {
        ImmutableSet<ViewDTO> views = ImmutableSet.of(sharedView, someView(), someView());
        when(viewService.forSearch("some-id")).thenReturn(views);
        return views;
    }

    private ViewDTO someView() {
        return ViewDTO.builder().id(UUID.randomUUID().toString()).searchId("doesnt matter").title("Rhapsody in Beige").state(new HashMap<>()).build();
    }
}
