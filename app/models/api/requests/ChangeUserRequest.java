/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
 *
 */
package models.api.requests;

import models.User;
import models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import java.util.List;

public class ChangeUserRequest extends ApiRequest {
    @Constraints.Required
    public String fullname;
    @Constraints.Required
    public String email;
    @Constraints.Required
    public List<String> permissions;

    public ChangeUserRequest() { /* for data binding */ }

    public ChangeUserRequest(User user) {
        this.fullname = user.getFullName();
        this.email = user.getEmail();
        this.permissions = user.getPermissions();
    }
}
