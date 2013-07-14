package controllers;

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

import static play.data.Form.form;

public class SessionsController extends Controller {
	private static final Logger log = LoggerFactory.getLogger(SessionsController.class);

	final static Form<LoginRequest> userForm = form(LoginRequest.class);
	
	public static Result index() {
		// Redirect if already logged in.
		if (session("username") != null && !session("username").isEmpty()) {
			return redirect("/");
		}
		return ok(views.html.sessions.login.render(userForm));
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
			subject.getSession();
			session("authSession", subject.getSession().toString());
			return redirect("/");
		} catch (AuthenticationException e) {
			log.warn("Unable to authenticate user. Redirecting back to '/'", e);
			flash("error", "Sorry, those credentials are invalid.");
			return redirect("/login");
		}
	}

	public static Result destroy() {
		session().clear();
		return redirect("/login");
	}
	
}
