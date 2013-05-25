package controllers;

import lib.Authenticator;
import models.LoginRequest;
import models.User;

import play.mvc.*;
import play.data.*;
import static play.data.Form.*;

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
		if (auth.authenticate(r.username, r.password)) {
			session("username", "LOL_SOME_USER_ID");
			return redirect("/");
		} else {
			flash("error", "Wrong username or password.");
			return redirect("/login");
		}
	}

	public static Result destroy() {
		session().clear();
		return redirect("/login");
	}
	
}
