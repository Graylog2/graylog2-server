/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.users;

import org.graylog2.database.PersistedService;
import org.graylog2.security.ldap.LdapEntry;
import org.graylog2.security.ldap.LdapSettings;

import java.util.List;

public interface UserService extends PersistedService {
    User load(String username);

    User create();

    List<User> loadAll();

    User syncFromLdapEntry(LdapEntry userEntry, LdapSettings ldapSettings, String username);

    void updateFromLdap(UserImpl user, LdapEntry userEntry, LdapSettings ldapSettings, String username);

    User getAdminUser();
}
