package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.Security.Authenticated;

@Authenticated
public class AuthenticatedController extends Controller {

	protected static User currentUser() {
		return User.load(session("username"));
	}
	
}
