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
import java.util.Set;
import java.util.function.Predicate;

import static org.graylog.plugins.views.search.views.ViewDTO.idsFrom;

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

    boolean isSearchPermitted(String id, User user, Predicate<ViewDTO> viewReadPermission) {
        final Collection<ViewDTO> views = viewService.forSearch(id);
        final Set<String> viewIds = idsFrom(views);

        if (views.isEmpty())
            return false;

        return hasSharedView(user, viewIds) || hasDirectReadPermissionForAny(views, viewReadPermission);
    }

    private boolean hasSharedView(User user, Set<String> viewIds) {
        return viewSharingService.forViews(viewIds).stream()
                .anyMatch(vs -> isViewSharedForUser.isAllowedToSee(user, vs));
    }

    private boolean hasDirectReadPermissionForAny(Collection<ViewDTO> views, Predicate<ViewDTO> viewReadPermission) {
        return views.stream().anyMatch(viewReadPermission);
    }
}
