package controllers;

import lib.security.RedirectAuthenticator;
import models.User;
import play.mvc.Controller;
import play.mvc.Security.Authenticated;

@Authenticated(RedirectAuthenticator.class)
public class AuthenticatedController extends Controller {

	protected static User currentUser() {
        return User.current();
	}
	
}
