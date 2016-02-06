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
package org.graylog2.shared.users;

import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.security.ldap.LdapSettings;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserService extends PersistedService {
    User load(String username);

    int delete(String username);

    User create();

    List<User> loadAll();

    User syncFromLdapEntry(LdapEntry userEntry, LdapSettings ldapSettings, String username);

    void updateFromLdap(User user, LdapEntry userEntry, LdapSettings ldapSettings, String username);

    User getAdminUser();

    long count();

    Collection<User> loadAllForRole(Role role);

    Set<String> getRoleNames(User user);

    List<String> getPermissionsForUser(User user);

    void dissociateAllUsersFromRole(Role role);
}
