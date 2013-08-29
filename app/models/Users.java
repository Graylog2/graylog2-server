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
package models;

import com.google.common.collect.Lists;
import lib.APIException;
import lib.Api;
import models.api.responses.system.UserResponse;
import models.api.responses.system.UsersListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class Users {
    private static final Logger log = LoggerFactory.getLogger(Users.class);

    public static List<User> all() {
        UsersListResponse response;
        try {
            response = Api.get("/users", UsersListResponse.class);
            List<User> users = Lists.newArrayList();
            for (UserResponse userResponse : response.users) {
                users.add(new User(userResponse, null)); // we don't have password's for the user list, obviously
            }
            return users;
        } catch (IOException e) {
            log.error("Could not retrieve list of users", e);
        } catch (APIException e) {
            log.error("Could not retrieve list of users", e);
        }
        return Lists.newArrayList();
    }
}
