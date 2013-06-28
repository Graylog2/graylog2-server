package controllers;

import lib.security.Authenticator;
import models.LoginRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

import static play.data.Form.form;

public class SessionsController extends Controller {

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
        Authenticator auth = new Authenticator();

		final Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(new UsernamePasswordToken(r.username, r.password));
			session("username", "LOL_SOME_USER_ID");
			return redirect("/");
		} catch (AuthenticationException e) {
			flash("error", "Go away.");
			return redirect("/login");
		}
//
//		if (auth.authenticate(r.username, r.password)) {
//			session("username", "LOL_SOME_USER_ID");
//			return redirect("/");
//		} else {
//			flash("error", "Wrong username or password.");
//			return redirect("/login");
//		}
	}

	public static Result destroy() {
		session().clear();
		return redirect("/login");
	}
	
}
