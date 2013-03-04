package controllers;

import play.mvc.*;

public class Dashboard extends AuthenticatedController {

    public static Result index() {
        return ok(currentUser().toString());
    }
  
}
