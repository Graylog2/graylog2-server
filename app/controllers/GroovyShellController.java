package controllers;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */

import play.mvc.Result;

public class GroovyShellController extends AuthenticatedController {
    public Result index() {
        return ok(views.html.system.groovy.index.render(currentUser()));
    }
}
