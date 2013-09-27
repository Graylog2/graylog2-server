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

import com.google.common.collect.Maps;
import lib.security.Graylog2ServerUnavailableException;
import models.LoginRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.HashMap;

import static play.data.Form.form;

public class SessionsController extends Controller {
	private static final Logger log = LoggerFactory.getLogger(SessionsController.class);

	final static Form<LoginRequest> userForm = form(LoginRequest.class);
	
	public Result index() {
        // Redirect if already logged in.
        final Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            log.debug("User {} already authenticated, redirecting to /", subject);
            redirect("/");
        }
        if (session("username") != null && !session("username").isEmpty()) {
            return redirect("/");
        }
        Form<LoginRequest> form = userForm;
        if (subject.isRemembered()) {
            final HashMap<String, String> prefilledForm = Maps.newHashMap();
            prefilledForm.put("username", subject.getPrincipal().toString());
            form = userForm.bind(prefilledForm, "username");
        }
        return ok(views.html.sessions.login.render(form));
    }
	
	public Result create() {
		Form<LoginRequest> loginRequest = userForm.bindFromRequest();

		if (loginRequest.hasErrors()) {
			flash("error", "Please fill out all fields.");
            return badRequest(views.html.sessions.login.render(loginRequest));
		}
		
		LoginRequest r = loginRequest.get();

		final Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(new UsernamePasswordToken(r.username, r.password));
			return redirect("/");
		} catch (AuthenticationException e) {
			log.warn("Unable to authenticate user {}. Redirecting back to '/'", r.username, e);
            if (e instanceof Graylog2ServerUnavailableException) {
                flash("error", "Could not reach any Graylog2 server!");
            } else {
                flash("error", "Sorry, those credentials are invalid.");
            }
			return badRequest(views.html.sessions.login.render(loginRequest));
		}
	}

	public Result destroy() {
        SecurityUtils.getSubject().logout();
		session().clear();
		return redirect("/login");
	}
	
}
