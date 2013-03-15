package controllers;

import play.mvc.*;

public class Dashboard extends AuthenticatedController {

    public static Result index() {
        return ok(views.html.dashboard.index.render(currentUser()));
    }
  
}
