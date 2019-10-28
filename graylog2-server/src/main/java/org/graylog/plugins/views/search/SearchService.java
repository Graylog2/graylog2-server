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
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.views.search.views.sharing.ViewSharingService;
import org.graylog2.plugin.database.users.User;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Predicate;

public class SearchService {
    private final SearchDbService dbService;
    private final ViewService viewService;
    private final ViewSharingService viewSharingService;
    private final IsViewSharedForUser isViewSharedForUser;

    @Inject
    public SearchService(SearchDbService dbService, ViewService viewService, ViewSharingService viewSharingService, IsViewSharedForUser isViewSharedForUser) {
        this.dbService = dbService;
        this.viewService = viewService;
        this.viewSharingService = viewSharingService;
        this.isViewSharedForUser = isViewSharedForUser;
    }

    public Optional<Search> getForUser(String id, User user, Predicate<String> permissionChecker) {
        final Optional<Search> search = dbService.get(id);

        if (!search.isPresent()) {
            return Optional.empty();
        }

        if (search.map(s -> s.owner().map(owner -> owner.equals(user.getName())).orElse(false)).orElse(false)) {
            return search;
        }

        if (viewService.forSearch(id).stream()
                .map(view -> viewSharingService.forView(view.id()))
                .anyMatch(viewSharing -> viewSharing.map(sharing -> isViewSharedForUser.isAllowedToSee(user, sharing)).orElse(false))) {
            return search;
        }

        if (viewService.forSearch(id).stream()
                .anyMatch(view -> permissionChecker.test(view.id()))) {
            return search;
        }

        return Optional.empty();
    }
}
