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
package models.api.requests;

import com.google.common.collect.Lists;
import models.User;

import java.util.List;

public class ChangeUserRequestForm extends ChangeUserRequest {

    public List<String> streampermissions = Lists.newArrayList();

    public List<String> streameditpermissions = Lists.newArrayList();

    public List<String> dashboardpermissions = Lists.newArrayList();

    public List<String> dashboardeditpermissions = Lists.newArrayList();

    public ChangeUserRequest toApiRequest() {
        final ChangeUserRequest r = new ChangeUserRequest();
        r.email = email;
        r.fullname = fullname;
        r.startpage = startpage;
        r.timezone = timezone;
        return r;
    }

    public ChangeUserRequestForm() {
        super();
    }

    public ChangeUserRequestForm(User user) {
        super(user);
    }
}
