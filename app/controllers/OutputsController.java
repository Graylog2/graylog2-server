package controllers;

import lib.BreadcrumbList;
import lib.security.RestPermissions;
import org.graylog2.restclient.models.OutputService;
import play.mvc.Result;
import views.helpers.Permissions;

import javax.inject.Inject;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputsController extends AuthenticatedController {
    private final OutputService outputService;

    @Inject
    public OutputsController(OutputService outputService) {
        this.outputService = outputService;
    }

    public Result index() {
        if (!Permissions.isPermitted(RestPermissions.OUTPUTS_READ)) {
            return redirect(routes.StartpageController.redirect());
        }

        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Outputs", routes.OutputsController.index());

        return ok(views.html.system.outputs.index.render(currentUser(), bc));
    }
}
