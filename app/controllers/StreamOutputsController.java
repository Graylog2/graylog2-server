package controllers;

import lib.BreadcrumbList;
import lib.security.RestPermissions;
import models.forms.AddOutputToStreamForm;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Output;
import org.graylog2.restclient.models.OutputService;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.api.requests.outputs.OutputLaunchRequest;
import play.data.Form;
import play.mvc.Result;
import views.helpers.Permissions;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static play.data.Form.form;
import static views.helpers.Permissions.isPermitted;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamOutputsController extends AuthenticatedController {
    private final StreamService streamService;

    @Inject
    public StreamOutputsController(StreamService streamService) {
        this.streamService = streamService;
    }

    public Result index(String streamId) throws IOException, APIException {
        if (!isPermitted(RestPermissions.STREAMS_READ, streamId) || !isPermitted(RestPermissions.OUTPUTS_READ))
            return forbidden();

        final Stream stream = streamService.get(streamId);

        if (stream == null) {
            flash("error", "Stream <" + streamId + "> does not exit!");
            return redirect(routes.StreamsController.index());
        }

        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("Streams", routes.StreamsController.index());
        bc.addCrumb("Outputs of " + stream.getTitle(), routes.StreamOutputsController.index(stream.getId()));

        return ok(views.html.streams.outputs.index.render(currentUser(),
                bc,
                stream));
    }
}
