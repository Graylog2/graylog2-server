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
package org.graylog2.security.realm;

import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.graylog2.plugin.database.users.User;

import java.util.Set;

public class UserAuthorizationInfo extends SimpleAuthorizationInfo {
    private final User user;

    public UserAuthorizationInfo(User user) {
        super();
        this.user = user;
    }

    public UserAuthorizationInfo(Set<String> roles, User user) {
        super(roles);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
