/*
 * Copyright 2014 TORCH UG
 *
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
import org.graylog2.restclient.models.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChangeUserRequestForm extends ChangeUserRequest {

    public List<String> streampermissions = Lists.newArrayList();

    public List<String> streameditpermissions = Lists.newArrayList();

    public List<String> dashboardpermissions = Lists.newArrayList();

    public List<String> dashboardeditpermissions = Lists.newArrayList();

    public boolean session_timeout_never;

    public String timeout_unit;

    public long timeout;

    public ChangeUserRequest toApiRequest() {
        final ChangeUserRequest r = new ChangeUserRequest();
        r.email = email;
        r.fullname = fullname;
        r.startpage = startpage;
        r.timezone = timezone.isEmpty() ? null : timezone;
        r.sessionTimeoutMs = sessionTimeoutMs;
        return r;
    }

    public ChangeUserRequestForm() {
        super();
    }

    public ChangeUserRequestForm(User user) {
        super(user);
        // -1 is "never"
        if (sessionTimeoutMs == -1) {
            session_timeout_never = true;
        } else {
            session_timeout_never = false;
            // find the longest TimeUnit that will accommodate the given milliseconds
            if (sessionTimeoutMs % TimeUnit.DAYS.toMillis(1) == 0) {
                timeout_unit = "days";
                timeout = TimeUnit.MILLISECONDS.toDays(sessionTimeoutMs);
            } else if (sessionTimeoutMs % TimeUnit.HOURS.toMillis(1) == 0) {
                timeout_unit = "hours";
                timeout = TimeUnit.MILLISECONDS.toHours(sessionTimeoutMs);
            } else if (sessionTimeoutMs % TimeUnit.MINUTES.toMillis(1) == 0) {
                timeout_unit = "minutes";
                timeout = TimeUnit.MILLISECONDS.toMinutes(sessionTimeoutMs);
            } else if (sessionTimeoutMs % TimeUnit.SECONDS.toMillis(1) == 0) {
                timeout_unit = "seconds";
                timeout = TimeUnit.MILLISECONDS.toSeconds(sessionTimeoutMs);
            }
        }
    }
}
