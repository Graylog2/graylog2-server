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

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.system.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Crypto;
import play.mvc.Http;

import java.io.IOException;
import java.util.StringTokenizer;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final ApiClient api;
    private final User.Factory userFactory;

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

    public static void setCurrent(User user) {
        // save the current user in the request for easy access
        Http.Context.current().args.put("currentUser", user);
        log.debug("Setting the request's current user to {}", user);
    }

    public User load(String username) {
        final User currentUser = current();
        if (username.equals(currentUser.getName())) {
            return currentUser;
        }
        // a different user was requested, go and fetch it from the server
        try {
            final UserResponse response = ApiClient.get(UserResponse.class).path("/users/{0}", username).execute();
            // TODO this user is not cached locally for now. we should be tracking REST requests.
            return userFactory.fromResponse(response, null);
        } catch (IOException e) {
            log.error("Could not load user " + username, e);
        } catch (APIException e) {
            log.info("User " + username + " does not exist", e);
        }
        return null;
	}

    public User authenticateSessionUser() {
        // is there a logged in user at all?
        final Http.Session session = Http.Context.current().session();
        final String sessionId = session.get("sessionid");
        if (sessionId == null) {
            // there is no authenticated user yet.
            log.info("Accessing the current user failed, there's no sessionid in the cookie.");
            return null;
        }
        final String userPassHash = Crypto.decryptAES(sessionId);
        final StringTokenizer tokenizer = new StringTokenizer(userPassHash, "\t");
        if (tokenizer.countTokens() != 2) {
            return null;
        }
        final String userName = tokenizer.nextToken();
        final String passwordSha1 = tokenizer.nextToken();

        // special case for the local admin user for the web interface
        if (userName != null) {
            final LocalAdminUser localAdminUser = LocalAdminUser.getInstance();
            if (userName.equals(localAdminUser.getName())) {
                setCurrent(localAdminUser);
                return localAdminUser;
            }
        }
        try {
            UserResponse response = ApiClient.get(UserResponse.class)
                    .credentials(userName, passwordSha1)
                    .path("/users/{0}", userName)
                    .execute();

            User currentUser = userFactory.fromResponse(response, passwordSha1);
            setCurrent(currentUser);
            return currentUser;
        } catch (IOException e) {
            log.error("Could not reach graylog2 server", e);
        } catch (APIException e) {
            log.error("Unauthorized to load user " + userName, e);
        }
        return null;
    }
}
