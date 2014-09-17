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

import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface User extends Persisted {
    boolean isReadOnly();

    String getFullName();

    String getName();

    void setName(String username);

    String getEmail();

    List<String> getPermissions();

    Map<String, Object> getPreferences();

    Map<String, String> getStartpage();

    long getSessionTimeoutMs();

    void setSessionTimeoutMs(long timeoutValue);

    void setPermissions(List<String> permissions);

    void setPreferences(Map<String, Object> preferences);

    void setEmail(String email);

    void setFullName(String fullname);

    String getHashedPassword();

    void setPassword(String password, String passwordSecret);

    boolean isUserPassword(String password, String passwordSecret);

    DateTimeZone getTimeZone();

    void setTimeZone(DateTimeZone timeZone);

    void setTimeZone(String timeZone);

    boolean isExternalUser();

    void setExternal(boolean external);

    void setStartpage(String type, String id);

    boolean isLocalAdmin();
}
