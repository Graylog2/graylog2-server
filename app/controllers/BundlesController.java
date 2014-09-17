package controllers;

import lib.BreadcrumbList;
import play.mvc.Result;

public class BundlesController extends AuthenticatedController {
    public Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Bundles", routes.BundlesController.index());
        return ok(views.html.system.bundles.index.render(currentUser(), bc));
    }
}
