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

import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.plugin.database.users.User;

import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Predicate;

class ViewPermissions {
    private final ViewService viewService;
    private final ViewSharingService viewSharingService;
    private final IsViewSharedForUser isViewSharedForUser;

    @Inject
    ViewPermissions(ViewService viewService, ViewSharingService viewSharingService, IsViewSharedForUser isViewSharedForUser) {
        this.viewService = viewService;
        this.viewSharingService = viewSharingService;
        this.isViewSharedForUser = isViewSharedForUser;
    }

    boolean isSearchPermitted(String id, User user, Predicate<String> viewReadPermission) {
        final Collection<ViewDTO> views = viewService.forSearch(id);

        if (views.isEmpty())
            return false;

        return hasSharedView(user, views) || hasDirectReadPermissionForAny(views, viewReadPermission);
    }

    private boolean hasSharedView(User user, Collection<ViewDTO> views) {
        return views.stream()
                .anyMatch(view -> isSharedWith(view, user));
    }

    private boolean isSharedWith(ViewDTO view, User user) {
        return viewSharingService.forView(view.id())
                .filter(viewSharing -> isViewSharedForUser.isAllowedToSee(user, viewSharing))
                .isPresent();
    }

    private boolean hasDirectReadPermissionForAny(Collection<ViewDTO> views, Predicate<String> viewReadPermission) {
        return views.stream()
                .anyMatch(view -> viewReadPermission.test(view.id()));
    }
}
