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
package org.graylog.plugins.views.search.views.sharing;

import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class SpecificUsersStrategy implements SharingStrategy<SpecificUsers> {
    @Override
    public boolean isAllowedToSee(@Nullable User user, @NotNull SpecificUsers viewSharing) {
        if (user == null) {
            return false;
        }
        return user.isLocalAdmin() || viewSharing.users().stream().anyMatch(userName -> user.getName().equals(userName));
    }
}
