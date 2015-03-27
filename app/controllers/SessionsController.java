/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import org.graylog2.rest.models.system.sessions.responses.SessionResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.lib.Graylog2ServerUnavailableException;
import lib.security.RedirectAuthenticator;
import models.LoginRequest;
import org.graylog2.restclient.models.SessionService;
import org.graylog2.restclient.models.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.libs.Crypto;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import javax.inject.Inject;
import java.io.IOException;

import static play.data.Form.form;

public class SessionsController extends BaseController {
	private static final Logger log = LoggerFactory.getLogger(SessionsController.class);

	final static Form<LoginRequest> userForm = form(LoginRequest.class);

    private final ServerNodes serverNodes;
    private final RedirectAuthenticator authenticator;
    private final SessionService sessionService;

    @Inject
    public SessionsController(ServerNodes serverNodes, RedirectAuthenticator authenticator, SessionService sessionService) {
        this.serverNodes = serverNodes;
        this.authenticator = authenticator;
        this.sessionService = sessionService;
    }

    public Result index(String destination) {
        // Redirect if already logged in.
        String loggedInUserName = authenticator.getUsername(ctx());
        if (loggedInUserName != null) {
            log.debug("User {} already authenticated, redirecting to /", loggedInUserName);
            if (destination != null && !destination.isEmpty()) {
                return redirect(destination);
            } else {
                return redirect(routes.StartpageController.redirect());
            }
        }
        if (session("username") != null && !session("username").isEmpty()) {
            if (destination != null && !destination.isEmpty()) {
                return redirect(destination);
            } else {
                return redirect(routes.StartpageController.redirect());
            }
        }
        checkServerConnections();
        return ok(views.html.sessions.login.render(userForm, !serverNodes.isConnected(), destination));
    }

    private void checkServerConnections() {
        if (!serverNodes.isConnected()) {
            flash("error", "No Graylog servers available. Cannot log in.");
        }
    }

    public Result create() {
		Form<LoginRequest> loginRequest = userForm.bindFromRequest();

		if (loginRequest.hasErrors()) {
			flash("error", "Please fill out all fields.");
            return badRequest(views.html.sessions.login.render(loginRequest, !serverNodes.isConnected(), loginRequest.field("destination").value()));
		}
		
		LoginRequest r = loginRequest.get();

        try {
            final SessionResponse sessionResponse = sessionService.create(r.username, r.password, request().remoteAddress());
            // if we have successfully created a session, we can save that id for the next request
            final String cookieContent = Crypto.encryptAES(r.username + "\t" + sessionResponse.sessionId());
            Http.Context.current().session().put("sessionid", cookieContent);

            // if we were redirected from somewhere else because the session had expired, redirect back to that page
            // otherwise use the configured startpage (or skip it if that was requested)
            if (r.destination != null && !r.destination.isEmpty()) {
                return redirect(r.destination);
            }
            // upon redirect, the auth layer will load the user with the given session and log the user in.
            return redirect(routes.StartpageController.redirect());
        } catch (APIException e) {
            log.warn("Unable to authenticate user {}. Redirecting back to '/'", r.username, e);
            if (e.getCause() instanceof Graylog2ServerUnavailableException) {
                checkServerConnections();
            } else {
                flash("error", "Sorry, those credentials are invalid.");
            }
        } catch (IOException e) {
            flash("error", "We discovered Graylog servers but could not reach any. Please check your log file.(IOException)");
            log.error("Error when trying to reach Graylog servers.", e);
        } catch (AuthenticationException e) {
        }
        return badRequest(views.html.sessions.login.render(loginRequest, !serverNodes.isConnected(), loginRequest.field("destination").value()));
	}

    @Security.Authenticated(RedirectAuthenticator.class)
    public Result destroy() {
        final String sessionId = UserService.current().getSessionId();
        try {
            if (sessionId != null) {
                sessionService.destroy(sessionId);
            }
        } catch (APIException | IOException e) {
            log.info("Unable to end session for user {}", UserService.current().getName());
        }
        SecurityUtils.getSubject().logout();
		session().clear();
		return redirect(routes.StartpageController.redirect());
	}
}
