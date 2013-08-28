package controllers;

import com.google.common.collect.Maps;
import lib.security.Graylog2ServerUnvavailableException;
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
	
	public static Result index() {
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
	
	public static Result create() {
		Form<LoginRequest> loginRequest = userForm.bindFromRequest();

		if (loginRequest.hasErrors()) {
			flash("error", "Please fill out all fields.");
			return redirect("/login");
		}
		
		LoginRequest r = loginRequest.get();

		final Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(new UsernamePasswordToken(r.username, r.password));
			return redirect("/");
		} catch (AuthenticationException e) {
			log.warn("Unable to authenticate user {}. Redirecting back to '/'", r.username, e);
            if (e instanceof Graylog2ServerUnvavailableException) {
                flash("error", "Could not reach any Graylog2 server!");
            } else {
                flash("error", "Sorry, those credentials are invalid.");
            }
			return redirect("/login");
		}
	}

	public static Result destroy() {
        SecurityUtils.getSubject().logout();
		session().clear();
		return redirect("/login");
	}
	
}
