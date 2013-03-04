package models;

import play.mvc.Result;
import play.mvc.Http.Context;
import play.mvc.Security.Authenticator;

public class RedirectAuthenticator extends Authenticator {

	public Result onUnauthorized(Context ctx) {
		return redirect(controllers.routes.Sessions.index());
	}
	
}
