/*
 * Copyright 2013 TORCH UG
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
package models;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import lib.APIException;
import lib.ApiClient;
import models.api.requests.ChangeUserRequest;
import models.api.requests.CreateUserRequest;
import models.api.responses.system.UserResponse;
import models.api.responses.system.UsersListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;

public class User {
	private static final Logger log = LoggerFactory.getLogger(User.class);

    @Deprecated
    private final String id;
    private final String name;
	private final String email;
	private final String fullName;
	private final List<String> permissions;

    private final String passwordHash;

    @AssistedInject
    public User(@Assisted UserResponse ur, @Assisted String passwordHash) {
        this(ur.id, ur.username, ur.email, ur.fullName, ur.permissions, passwordHash);
    }

	public User(String id, String name, String email, String fullName, List<String> permissions, String passwordHash) {
        this.id = id;
        this.name = name;
		this.email = email;
		this.fullName = fullName;
		this.permissions = permissions;
        this.passwordHash = passwordHash;
    }

    public static List<User> all() {
        UsersListResponse response;
        try {
            response = ApiClient.get(UsersListResponse.class).path("/users").execute();
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

    public static void create(CreateUserRequest request) {
        try {
            ApiClient.post().path("/users").body(request).expect(Http.Status.CREATED).execute();
        } catch (APIException e) {
            log.error("Unable to create user", e);
        } catch (IOException e) {
            log.error("Unable to create user", e);
        }
    }

    public void update(ChangeUserRequest request) {
        try {
            ApiClient.put().path("/users/{0}", getName()).body(request).expect(Http.Status.NO_CONTENT).execute();
        } catch (APIException e) {
            log.error("Unable to update user", e);
        } catch (IOException e) {
            log.error("Unable to update user", e);
        }
    }
    @Deprecated
    public String getId() {
        return getName();
    }

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public List<String> getPermissions() {
        if (permissions == null) {
            return Lists.newArrayList();
        }
		return permissions;
	}

    public String getPasswordHash() {
        return passwordHash;
    }

    public interface Factory {
        User fromResponse(UserResponse ur, String passwordHash);
    }

}
