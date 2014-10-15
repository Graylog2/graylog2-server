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
package org.graylog2.restclient.models.api.requests;

import org.graylog2.restclient.models.User;
import org.joda.time.DateTimeZone;

import static play.data.validation.Constraints.Required;

public class CreateUserRequest extends ChangeUserRequest {
    @Required
    public String username;
    @Required
    public String password;

    public CreateUserRequest() { /* required for data binding */ }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public CreateUserRequest(User user) {
        this.username = user.getName();
        this.fullname = user.getFullName();
        this.email = user.getEmail();
        this.password = "";
        this.permissions  = user.getPermissions();
        final DateTimeZone timeZone = user.getTimeZone();
        if (timezone != null) {
            this.timezone = timeZone.getID();
        }
        this.sessionTimeoutMs = user.getSessionTimeoutMs();
    }

    public CreateUserRequest(CreateUserRequest c) {
        username = c.username;
        password = c.password;
        fullname = c.fullname;
        email = c.email;
        permissions = c.permissions;
        timezone = c.timezone;
        startpage = c.startpage;
        sessionTimeoutMs = c.sessionTimeoutMs;
    }
}
