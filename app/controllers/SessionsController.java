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
package controllers;

import com.google.inject.Inject;
import lib.ServerNodes;
import lib.security.Graylog2ServerUnavailableException;
import lib.security.RedirectAuthenticator;
import models.LoginRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.mvc.Result;

import static play.data.Form.form;

public class SessionsController extends BaseController {
	private static final Logger log = LoggerFactory.getLogger(SessionsController.class);

	final static Form<LoginRequest> userForm = form(LoginRequest.class);

    @Inject
    ServerNodes serverNodes;
	@Inject
    RedirectAuthenticator authenticator;

	public Result index() {
        // Redirect if already logged in.
        String loggedInUserName = authenticator.getUsername(ctx());
        if (loggedInUserName != null) {
            log.debug("User {} already authenticated, redirecting to /", loggedInUserName);
            return redirect("/");
        }
        if (session("username") != null && !session("username").isEmpty()) {
            return redirect("/");
        }
        checkServerConnections();
        return ok(views.html.sessions.login.render(userForm, !serverNodes.isConnected()));
    }

    private void checkServerConnections() {
        if (!serverNodes.isConnected()) {
            flash("error", "No Graylog2 servers available. Cannot log in.");
        }
    }

    public Result create() {
		Form<LoginRequest> loginRequest = userForm.bindFromRequest();

		if (loginRequest.hasErrors()) {
			flash("error", "Please fill out all fields.");
            return badRequest(views.html.sessions.login.render(loginRequest, !serverNodes.isConnected()));
		}
		
		LoginRequest r = loginRequest.get();

		final Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(new UsernamePasswordToken(r.username, r.password, request().remoteAddress()));
			return redirect("/");
		} catch (AuthenticationException e) {
			log.warn("Unable to authenticate user {}. Redirecting back to '/'", r.username, e);
            if (e instanceof Graylog2ServerUnavailableException) {
                checkServerConnections();
            } else {
                flash("error", "Sorry, those credentials are invalid.");
            }
			return badRequest(views.html.sessions.login.render(loginRequest, !serverNodes.isConnected()));
		}
	}

	public Result destroy() {
        SecurityUtils.getSubject().logout();
		session().clear();
		return redirect("/login");
	}
	
}
