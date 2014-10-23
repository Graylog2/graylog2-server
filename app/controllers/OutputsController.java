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
        try {
            if (!Permissions.isPermitted(RestPermissions.OUTPUTS_READ)) {
                return redirect(routes.StartpageController.redirect());
            }
            List<Output> outputs = outputService.list();

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Outputs", routes.OutputsController.index());

            return ok(views.html.system.outputs.index.render(
                    currentUser(),
                    bc,
                    outputs,
                    outputService.available().types
            ));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result create() throws APIException, IOException {
        if (!Permissions.isPermitted(RestPermissions.OUTPUTS_EDIT)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Form<OutputLaunchRequest> outputForm = form(OutputLaunchRequest.class).bindFromRequest();
        final OutputLaunchRequest request = outputForm.get();

        final Output output = outputService.create(request);

        flash("success", "Output \"" + output.getTitle() + "\" has been created!");
        return redirect(routes.OutputsController.index());
    }

    public Result terminate(String outputId, String redirectToStream) throws APIException, IOException {
        if (!Permissions.isPermitted(RestPermissions.OUTPUTS_TERMINATE)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Output output = outputService.get(outputId);
        if (output == null) {
            flash("error", "No such output!");
        } else {
            outputService.delete(outputId);
            flash("success", "Output \"" + output.getTitle() + "\" has been deleted!");
        }

        if (redirectToStream != null && !redirectToStream.isEmpty()) {
            return redirect(routes.StreamOutputsController.index(redirectToStream));
        } else {
            return redirect(routes.OutputsController.index());
        }
    }
}
