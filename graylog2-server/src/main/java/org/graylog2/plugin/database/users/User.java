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
package org.graylog2.plugin.database.users;

import org.graylog2.plugin.database.Persisted;
import org.graylog2.rest.models.users.requests.Startpage;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface User extends Persisted {
    boolean isReadOnly();

    String getFullName();

    String getName();

    void setName(String username);

    /**
     * Returns the email address of the user.
     *
     * Depending on how the user has been created, it is possible that the returned string contains multiple email
     * addresses separated by a "," character. (i.e. LDAP users)
     *
     * @return the email address
     */
    String getEmail();

    List<String> getPermissions();

    Map<String, Object> getPreferences();

    Startpage getStartpage();

    long getSessionTimeoutMs();

    void setSessionTimeoutMs(long timeoutValue);

    void setPermissions(List<String> permissions);

    void setPreferences(Map<String, Object> preferences);

    void setEmail(String email);

    void setFullName(String fullname);

    String getHashedPassword();

    void setPassword(String password);

    boolean isUserPassword(String password);

    DateTimeZone getTimeZone();

    void setTimeZone(DateTimeZone timeZone);

    void setTimeZone(String timeZone);

    boolean isExternalUser();

    void setExternal(boolean external);

    void setStartpage(String type, String id);

    void setStartpage(Startpage startpage);

    boolean isLocalAdmin();

    @Nonnull
    Set<String> getRoleIds();

    void setRoleIds(Set<String> roles);

    boolean isServiceAccount();

    void setServiceAccount(boolean isServiceAccount);
}
