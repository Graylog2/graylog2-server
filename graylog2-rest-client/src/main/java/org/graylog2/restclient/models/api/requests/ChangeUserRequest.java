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

import com.google.common.collect.Lists;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.User;
import play.data.validation.Constraints;

import java.util.List;

public class ChangeUserRequest extends ApiRequest {
    @Constraints.Required
    @JsonProperty("full_name")
    public String fullname;
    @Constraints.Required
    public String email;
    public List<String> permissions = Lists.newArrayList();

    public String timezone;

    public ChangeStartpageRequest startpage;

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @JsonProperty("session_timeout_ms")
    public long sessionTimeoutMs;

    public ChangeUserRequest() { /* for data binding */ }

    public ChangeUserRequest(User user) {
        this.fullname = user.getFullName();
        this.email = user.getEmail();
        this.permissions = user.getPermissions();
        if (user.getTimeZone() != null) {
            this.timezone = user.getTimeZone().getID();
        }

        this.startpage = new ChangeStartpageRequest();

        if(user.getStartpage() != null) {
            this.startpage.type = user.getStartpage().getType().toString().toLowerCase();
            this.startpage.id = user.getStartpage().getId();
        }
        this.sessionTimeoutMs = user.getSessionTimeoutMs();
    }
}
