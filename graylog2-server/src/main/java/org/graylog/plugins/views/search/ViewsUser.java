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

import org.graylog2.plugin.database.users.User;

import java.util.Objects;
import java.util.function.Predicate;

public class ViewsUser {
    private final User dbUser;
    private final Predicate<String> hasStreamReadPermission;
    private final Predicate<String> hasViewReadPermission;
    private final String name;
    private final boolean isAdmin;

    public static ViewsUser fromDbUser(User dbUser, Predicate<String> hasStreamReadPermission, Predicate<String> hasViewReadPermission, Predicate<String> hasPermission) {
        Objects.requireNonNull(dbUser);
        return new ViewsUser(dbUser, hasStreamReadPermission, hasViewReadPermission, hasPermission);
    }

    public ViewsUser(User dbUser, Predicate<String> hasStreamReadPermission, Predicate<String> hasViewReadPermission, Predicate<String> hasPermission) {
        Objects.requireNonNull(dbUser);
        this.dbUser = dbUser;
        this.name = dbUser.getName();
        this.isAdmin = dbUser.isLocalAdmin() || hasPermission.test("*");
        this.hasStreamReadPermission = hasStreamReadPermission;
        this.hasViewReadPermission = hasViewReadPermission;
    }

    public String getName() {
        return name;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isOwnerOf(Search search) {
        return search.owner()
                .map(owner -> owner.equals(name))
                .orElse(true);
    }

    public boolean hasStreamReadPermission(String streamId) {
        return hasStreamReadPermission.test(streamId);
    }

    public boolean hasViewReadPermission(String viewId) {
        return hasViewReadPermission.test(viewId);
    }

    //this is in here for compatibility with legacy code which depend on that class. should be removed, if possible.
    public User getDbUser() {
        return dbUser;
    }
}
