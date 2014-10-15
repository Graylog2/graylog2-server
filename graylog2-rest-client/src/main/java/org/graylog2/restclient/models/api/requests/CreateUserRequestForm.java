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

import java.util.concurrent.TimeUnit;

public class CreateUserRequestForm extends CreateUserRequest {

    // these exist for form binding, not existent in API!
    public boolean admin;

    public boolean session_timeout_never = false;

    public long timeout = 8;

    public String timeout_unit = "hours";

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isSession_timeout_never() {
        return session_timeout_never;
    }

    public void setSession_timeout_never(boolean session_timeout_never) {
        this.session_timeout_never = session_timeout_never;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getTimeout_unit() {
        return timeout_unit;
    }

    public void setTimeout_unit(String timeout_unit) {
        this.timeout_unit = timeout_unit;
    }

    public CreateUserRequest toApiRequest() {
        final CreateUserRequest request = new CreateUserRequest(this);
        // -1 is "never"
        if (session_timeout_never) {
            request.sessionTimeoutMs = -1;
        } else {
            request.sessionTimeoutMs = TimeUnit.valueOf(timeout_unit.toUpperCase()).toMillis(timeout);
        }
        return request;
    }
}
