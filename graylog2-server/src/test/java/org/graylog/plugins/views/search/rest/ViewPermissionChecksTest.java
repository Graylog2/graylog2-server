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

import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharing;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ViewPermissionChecksTest {
    private ViewSharingService viewSharingService;
    private IsViewSharedForUser isViewSharedForUser;
    private ViewDTO view;
    private User user;
    private BiPredicate<String, String> notPermitted = (p, id) -> false;

    private ViewPermissionChecks viewPermissionChecks;

    @BeforeEach
    void setUp() {
        this.viewSharingService = mock(ViewSharingService.class);
        this.isViewSharedForUser = mock(IsViewSharedForUser.class);
        this.view = mock(ViewDTO.class);
        this.user = mock(User.class);

        this.viewPermissionChecks = new ViewPermissionChecks(viewSharingService, isViewSharedForUser);
    }

    @Test
    void allowedToSeeSavedSearchReturnsTrueIfUserHasPermission() {
        when(view.id()).thenReturn("view-id");
        final BiPredicate<String, String> isPermitted = returnTrueFor(ViewsRestPermissions.VIEW_READ, "view-id");

        final boolean result = this.viewPermissionChecks.allowedToSeeSavedSearch(user, view, isPermitted);

        assertThat(result).isTrue();
    }

    @Test
    void allowedToSeeSavedSearchReturnsTrueIfUserIsOwner() {
        when(view.id()).thenReturn("view-id");
        when(view.owner()).thenReturn(Optional.of("hans-peter"));
        when(user.getName()).thenReturn("hans-peter");

        final boolean result = this.viewPermissionChecks.allowedToSeeSavedSearch(user, view, notPermitted);

        assertThat(result).isTrue();
    }

    @Test
    void allowedToSeeSavedSearchReturnsTrueIfSharedWithUser() {
        when(view.id()).thenReturn("view-id");
        when(view.owner()).thenReturn(Optional.empty());
        final ViewSharing viewSharing = mock(ViewSharing.class);
        when(viewSharingService.forView("view-id")).thenReturn(Optional.of(viewSharing));
        when(isViewSharedForUser.isAllowedToSee(user, viewSharing)).thenReturn(true);

        final boolean result = this.viewPermissionChecks.allowedToSeeSavedSearch(user, view, notPermitted);

        assertThat(result).isTrue();
    }

    @Test
    void allowedToSeeSavedSearchReturnsFalsePerDefault() {
        assertThat(this.viewPermissionChecks.allowedToSeeSavedSearch(user, view, notPermitted)).isFalse();

        when(view.id()).thenReturn("view-id");
        when(user.getName()).thenReturn("frank");
        when(view.owner()).thenReturn(Optional.of("hans-peter"));

        assertThat(this.viewPermissionChecks.allowedToSeeSavedSearch(user, view, notPermitted)).isFalse();

        final ViewSharing viewSharing = mock(ViewSharing.class);
        when(viewSharingService.forView("view-id")).thenReturn(Optional.of(viewSharing));
        when(isViewSharedForUser.isAllowedToSee(user, viewSharing)).thenReturn(false);

        assertThat(this.viewPermissionChecks.allowedToSeeSavedSearch(user, view, notPermitted)).isFalse();
    }

    @Test
    void allowedToSeeDashboardReturnsTrueIfUserHasPermission() {
        when(view.id()).thenReturn("view-id");
        final BiPredicate<String, String> isPermittedToSeeView = returnTrueFor(ViewsRestPermissions.VIEW_READ, "view-id");

        assertThat(this.viewPermissionChecks.allowedToSeeDashboard(user, view, isPermittedToSeeView)).isTrue();

        final BiPredicate<String, String> isPermittedToSeeDashboard = returnTrueFor(RestPermissions.DASHBOARDS_READ, "view-id");

        assertThat(this.viewPermissionChecks.allowedToSeeDashboard(user, view, isPermittedToSeeDashboard)).isTrue();
    }

    @Test
    void allowedToSeeDashboardReturnsTrueIfUserIsOwner() {
        when(view.id()).thenReturn("view-id");
        when(view.owner()).thenReturn(Optional.of("hans-peter"));
        when(user.getName()).thenReturn("hans-peter");

        final boolean result = this.viewPermissionChecks.allowedToSeeDashboard(user, view, notPermitted);

        assertThat(result).isTrue();
    }

    @Test
    void allowedToSeeDashboardReturnsTrueIfSharedWithUser() {
        when(view.id()).thenReturn("view-id");
        when(view.owner()).thenReturn(Optional.empty());
        final ViewSharing viewSharing = mock(ViewSharing.class);
        when(viewSharingService.forView("view-id")).thenReturn(Optional.of(viewSharing));
        when(isViewSharedForUser.isAllowedToSee(user, viewSharing)).thenReturn(true);

        final boolean result = this.viewPermissionChecks.allowedToSeeDashboard(user, view, notPermitted);

        assertThat(result).isTrue();
    }

    @Test
    void allowedToSeeDashboardReturnsFalsePerDefault() {
        assertThat(this.viewPermissionChecks.allowedToSeeDashboard(user, view, notPermitted)).isFalse();

        when(view.id()).thenReturn("view-id");
        when(user.getName()).thenReturn("frank");
        when(view.owner()).thenReturn(Optional.of("hans-peter"));

        assertThat(this.viewPermissionChecks.allowedToSeeDashboard(user, view, notPermitted)).isFalse();

        final ViewSharing viewSharing = mock(ViewSharing.class);
        when(viewSharingService.forView("view-id")).thenReturn(Optional.of(viewSharing));
        when(isViewSharedForUser.isAllowedToSee(user, viewSharing)).thenReturn(false);

        assertThat(this.viewPermissionChecks.allowedToSeeDashboard(user, view, notPermitted)).isFalse();
    }

    private BiPredicate<String, String> returnTrueFor(String expectedPermission, String expectedId) {
        return (permission, id) -> permission.equals(expectedPermission) && id.equals(expectedId);
    }
}
