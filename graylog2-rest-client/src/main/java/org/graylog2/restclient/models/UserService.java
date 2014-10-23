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
/*
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
package org.graylog2.restclient.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Graylog2ServerUnavailableException;
import org.graylog2.restclient.models.api.requests.ApiRequest;
import org.graylog2.restclient.models.api.requests.CreateUserRequest;
import org.graylog2.restclient.models.api.responses.system.UserResponse;
import org.graylog2.restclient.models.api.responses.system.UsersListResponse;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.graylog2.restroutes.generated.UsersResource;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final ApiClient api;
    private final User.Factory userFactory;
    private final UsersResource resource = routes.UsersResource();

    @Inject
    private UserService(ApiClient api, User.Factory userFactory) {
        this.api = api;
        this.userFactory = userFactory;
    }


    public static User current() {
        try {
            return (User) Http.Context.current().args.get("currentUser");
        } catch (RuntimeException e) {
            // Http.Context.current() throws a plain RuntimeException if there's no context,
            // for example in background threads.
            // That is fine, because we don't have a current user in those scenarios anyway.
            return null;
        }
    }

    public static String currentSessionId() {
        try {
            return (String) Http.Context.current().args.get("sessionId");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void setCurrent(User user) {
        // save the current user in the request for easy access
        Http.Context.current().args.put("currentUser", user);
        log.debug("Setting the request's current user to {}", user);
    }

    public List<User> all() {
        UsersListResponse response;
        try {
            response = api.path(routes.UsersResource().listUsers(), UsersListResponse.class).execute();
            List<User> users = Lists.newArrayList();
            for (UserResponse userResponse : response.users) {
                users.add(new User(api, userResponse, null)); // we don't have password's for the user list, obviously
            }
            return users;
        } catch (IOException e) {
            log.error("Could not retrieve list of users", e);
        } catch (APIException e) {
            log.error("Could not retrieve list of users", e);
        }
        return Collections.emptyList();
    }

    public boolean create(CreateUserRequest request) {
        try {
            api.path(resource.create()).body(request).expect(Http.Status.CREATED).execute();
            return true;
        } catch (APIException e) {
            log.error("Unable to create user", e);
            return false;
        } catch (IOException e) {
            log.error("Unable to create user", e);
            return false;
        }
    }

    public boolean savePreferences(String username, Map<String, Object> preferences) {
        try {
            ObjectMapper m = new ObjectMapper();
            final String body = m.writeValueAsString(preferences);
            api.path(resource.savePreferences(username)).body(new ApiRequest() {
                public String toJson() {
                    return body;
                }
            }).expect(Http.Status.NO_CONTENT).execute();
        } catch (Exception e) {
            log.error("Could not save preferences for " + username, e);
            return false;
        }
        return true;

    }

    public User load(String username) {
        final User currentUser = current();
        if (username.equals(currentUser.getName())) {
            return currentUser;
        }
        // a different user was requested, go and fetch it from the server
        try {
            final UserResponse response = api.path(resource.get(username), UserResponse.class).execute();
            // TODO this user is not cached locally for now. we should be tracking REST requests.
            // TODO we cache the user for this request, but only for checking permissions. this needs to be cleaned up after 0.20.0
            final User user = userFactory.fromResponse(response, null);
            Http.Context.current().args.put("perRequestUsersCache:" + user.getName(), user);
            return user;
        } catch (IOException e) {
            log.error("Could not load user " + username, e);
        } catch (APIException e) {
            log.info("User " + username + " does not exist", e);
        }
        return null;
    }

    public User retrieveUserWithSessionId(String userName, String sessionId) {
        try {
            UserResponse response = api.path(resource.get(userName), UserResponse.class)
                    .session(sessionId)
                    .execute();

            User currentUser = userFactory.fromResponse(response, sessionId);
            currentUser.setSubject(new Subject.Builder()
                    .principals(new SimplePrincipalCollection(currentUser.getName(), "REST session realm"))
                    .authenticated(true)
                    .buildSubject());
            setCurrent(currentUser);
            return currentUser;
        } catch (IOException e) {
            log.error("Could not reach graylog2 server", e);
        } catch (APIException e) {
            log.error("Unauthorized to load user " + userName, e);
        } catch (Graylog2ServerUnavailableException e) {
            // this leads to a different return code in RedirectAuthenticator.
            throw e;
        }
        return null;
    }

    public void delete(String username) {
        try {
            api.path(resource.deleteUser(username)).expect(Http.Status.NO_CONTENT).execute();
        } catch (APIException e) {
            log.error("Unable to delete user " + username, e);
        } catch (IOException e) {
            log.error("Unable to delete user " + username, e);
        }
    }

    public List<User> allExceptAdmin() {
        List<User> result = Lists.newArrayList();

        for(User user : all()) {
            if (!user.getName().equals("admin")) {
                result.add(user);
            }
        }

        return result;
    }
}
