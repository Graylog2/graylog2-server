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

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Optional;
import java.util.function.BiPredicate;

public class ViewPermissionChecks {
    private final ViewSharingService viewSharingService;
    private final IsViewSharedForUser isViewSharedForUser;

    @Inject
    public ViewPermissionChecks(ViewSharingService viewSharingService, IsViewSharedForUser isViewSharedForUser) {
        this.viewSharingService = viewSharingService;
        this.isViewSharedForUser = isViewSharedForUser;
    }

    boolean allowedToSeeSavedSearch(User user, ViewDTO savedSearch, BiPredicate<String, String> isPermitted) {
        return isPermitted.test(ViewsRestPermissions.VIEW_READ, savedSearch.id())
                || ownsView(user, savedSearch)
                || isSharedWithUser(user, savedSearch);
    }

    boolean allowedToSeeDashboard(User user, ViewDTO dashboard, BiPredicate<String, String> isPermitted) {
        return isPermitted.test(ViewsRestPermissions.VIEW_READ, dashboard.id())
                || isPermitted.test(RestPermissions.DASHBOARDS_READ, dashboard.id())
                || ownsView(user, dashboard)
                || isSharedWithUser(user, dashboard);
    }

    boolean allowedToSeeView(User user, ViewDTO view, BiPredicate<String, String> isPermitted) {
        return isDashboard(view)
                ? allowedToSeeDashboard(user, view, isPermitted)
                : allowedToSeeSavedSearch(user, view, isPermitted);
    }

    boolean ownsView(@Nullable User currentUser, @Nullable ViewDTO view) {
        if (currentUser == null || view == null) {
            return false;
        }

        final String name = currentUser.getName();
        if (name == null) {
            return false;
        }

        return view.owner()
                .map(name::equals)
                .orElse(false);
    }

    private boolean isSharedWithUser(User user, ViewDTO view) {
        final Optional<ViewSharing> viewSharing = viewSharingService.forView(view.id());
        return viewSharing.map(sharing -> isViewSharedForUser.isAllowedToSee(user, sharing)).orElse(false);
    }

    boolean isDashboard(ViewDTO view) {
        return view != null && view.type().equals(ViewDTO.Type.DASHBOARD);
    }
}
