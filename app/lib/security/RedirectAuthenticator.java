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
package lib.security;

import com.ning.http.util.Base64;
import controllers.routes;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.Graylog2ServerUnavailableException;
import org.graylog2.restclient.models.SessionService;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.graylog2.restclient.models.api.responses.SessionCreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Crypto;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security.Authenticator;

import javax.inject.Inject;
import java.io.IOException;
import java.util.StringTokenizer;

public class RedirectAuthenticator extends Authenticator {
    private static final Logger log = LoggerFactory.getLogger(RedirectAuthenticator.class);
    public static final String GRAYLOG_2_SERVER_MISSING_KEY = "GRAYLOG2SERVERMISSING";

    // TODO crutch, we need to write out own AuthenticatedAction filter... :(
    @Inject
    public static UserService userService;
    @Inject
    public static SessionService sessionService;

    @Override
    public String getUsername(Context ctx) {
        try {
            final User sessionUser = pipelineAuths(authenticateSessionUser(), authenticateBasicAuthUser());
            if (sessionUser == null) {
                return null;
            }
            return sessionUser.getName();
        } catch (Graylog2ServerUnavailableException e) {
            ctx.args.put(GRAYLOG_2_SERVER_MISSING_KEY, e);
            return null;
        }
    }

    @Override
	public Result onUnauthorized(Context ctx) {
        if (ctx.args.containsKey(GRAYLOG_2_SERVER_MISSING_KEY)) {
            // the client is not "unauthorized" per se, but we couldn't reach any backend to authenticate against.
            // if this is a XMLHttpRequest, we respond with a 504 Gateway Timeout, to trigger the error handlers in Javascript
            final String xhr = ctx.request().getHeader("X-Requested-With");
            if ("xmlhttprequest".equalsIgnoreCase(xhr)) {
                return status(Http.Status.GATEWAY_TIMEOUT);
            }

            // otherwise we couldn't reach any backend, and need to tell the user so.
            // we redirect to a special controller, which won't try to load user.
            ctx.session().put("disconnected_orig_uri", ctx.request().uri());
            return redirect(routes.LonesomeInterfaceController.index());
        } else {
            // XMLHttpRequests should still get a 401 if the backend server couldn't authenticate, because the session
            // has most likely expired.
            // As such, the XHR code in the web interface should then redirect to /
            final String xhr = ctx.request().getHeader("X-Requested-With");
            if (xhr != null && xhr.equalsIgnoreCase("xmlhttprequest")) {
                return status(Http.Status.UNAUTHORIZED);
            }

        }
        final String destination = ctx.request().uri();
        // don't redirect to /logout directly after login ;)
        if (routes.SessionsController.destroy().url().equals(destination)) {
            return redirect(controllers.routes.SessionsController.index(""));
        }
        return redirect(controllers.routes.SessionsController.index(destination));
	}

    public User authenticateSessionUser() {
        // is there a logged in user at all?
        final Http.Session session = Http.Context.current().session();
        final String encryptedSessionId = session.get("sessionid");
        if (encryptedSessionId == null) {
            // there is no authenticated user yet.
            log.info("Accessing the current user failed, there's no sessionid in the cookie.");
            return null;
        }
        final String userAndSessionId = Crypto.decryptAES(encryptedSessionId);
        final StringTokenizer tokenizer = new StringTokenizer(userAndSessionId, "\t");
        if (tokenizer.countTokens() != 2) {
            return null;
        }
        final String userName = tokenizer.nextToken();
        final String sessionId = tokenizer.nextToken();
        Http.Context.current().args.put("sessionId", sessionId);
        // special case for the local admin user for the web interface
//        if (userName != null) {
//            final LocalAdminUser localAdminUser = LocalAdminUser.getInstance();
//            if (userName.equals(localAdminUser.getName())) {
//                setCurrent(localAdminUser);
//                return localAdminUser;
//            }
//        }
        return userService.retrieveUserWithSessionId(userName, sessionId);
    }

    protected User authenticateBasicAuthUser() {
        final Http.Request request = Http.Context.current().request();
        final String authorizationHeader = request.getHeader("authorization");

        if (authorizationHeader == null)
            return null;

        final String authToken = authorizationHeader.split(" ")[1];

        if (authToken == null)
            return null;

        byte[] decodedAuth;
        String[] credString = null;
        try {
            decodedAuth = Base64.decode(authToken);
            credString = new String(decodedAuth, "UTF-8").split(":");
        } catch (IOException e) {
            log.error("Unable to decode basic auth information: ", e);
            return null;
        }

        if (credString == null || credString.length != 2)
            return null;

        final String userName = credString[0];
        final String password = credString[1];

        try {
            SessionCreateResponse session = sessionService.create(userName, password, request.remoteAddress());
            return userService.retrieveUserWithSessionId(userName, session.sessionId);
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

    protected User pipelineAuths(User... authResults) {
        for (User user : authResults)
            if (user != null)
                return user;
        return null;
    }
}
