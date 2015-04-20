package controllers;

import lib.BreadcrumbList;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.Output;
import org.graylog2.restclient.models.OutputService;
import org.graylog2.restclient.models.api.requests.outputs.OutputLaunchRequest;
import org.graylog2.restclient.models.api.responses.AvailableOutputSummary;
import org.graylog2.restclient.models.api.responses.system.OutputSummaryResponse;
import play.data.Form;
import play.mvc.Result;
import views.helpers.Permissions;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static play.data.Form.form;

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
