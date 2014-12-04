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

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamOutputsController extends AuthenticatedController {
    private final StreamService streamService;
    private final OutputService outputService;

    @Inject
    public StreamOutputsController(StreamService streamService,
                                   OutputService outputService) {
        this.streamService = streamService;
        this.outputService = outputService;
    }

    public Result index(String streamId) throws IOException, APIException {
        final Stream stream = streamService.get(streamId);

        if (stream == null) {
            flash("error", "Stream <" + streamId + "> does not exit!");
            return redirect(routes.StreamsController.index());
        }

        List<Output> outputs = streamService.getOutputs(streamId);

        List<Output> otherOutputs = outputService.list();
        otherOutputs.removeAll(outputs);

        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("Streams", routes.StreamsController.index());
        bc.addCrumb("Outputs of " + stream.getTitle(), routes.StreamOutputsController.index(stream.getId()));

        return ok(views.html.streams.outputs.index.render(currentUser(),
                bc,
                outputs,
                otherOutputs,
                stream,
                outputService.available().types));
    }

    public Result create(String streamId) throws APIException, IOException {
        if (!Permissions.isPermitted(RestPermissions.OUTPUTS_EDIT)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Stream stream = streamService.get(streamId);

        if (stream == null) {
            flash("error", "Stream <" + streamId + "> does not exist!");
            return redirect(routes.StreamsController.index());
        }

        final Form<OutputLaunchRequest> outputForm = form(OutputLaunchRequest.class).bindFromRequest();
        final OutputLaunchRequest request = outputForm.get();

        final Output output = outputService.create(request);

        streamService.addOutput(streamId, output.getId());

        flash("success", "Output " + output.getTitle() + " has been created!");
        return redirect(routes.StreamOutputsController.index(streamId));
    }

    public Result add(String streamId) throws IOException, APIException {
        final Form<AddOutputToStreamForm> form = form(AddOutputToStreamForm.class).bindFromRequest();
        final AddOutputToStreamForm request = form.get();
        String outputId = request.outputId;

        if (outputId == null) {
            flash("error", "No output selected!");
            return redirect(routes.StreamOutputsController.index(streamId));
        }

        final Stream stream = streamService.get(streamId);

        if (stream == null) {
            flash("error", "Stream <" + streamId + "> does not exist!");
            return redirect(routes.StreamsController.index());
        }

        final Output output = outputService.get(outputId);

        if (output == null) {
            flash("error", "Output <" + outputId + "> does not exist!");
            return redirect(routes.StreamsController.index());
        }

        streamService.addOutput(streamId, outputId);

        flash("succes", "Output <" + output.getTitle() + "> has been added to Stream!");

        return redirect(routes.StreamOutputsController.index(streamId));
    }
    public Result remove(String streamId, String outputId) throws APIException, IOException {
        final Stream stream = streamService.get(streamId);

        if (stream == null) {
            flash("error", "Stream <" + streamId + "> does not exist!");
            return redirect(routes.StreamsController.index());
        }

        final Output output = outputService.get(outputId);

        if (output == null) {
            flash("error", "Output <" + outputId + "> does not exist!");
            return redirect(routes.StreamsController.index());
        }

        streamService.removeOutput(streamId, outputId);

        flash("succes", "Output <" + output.getTitle() + "> has been removed to Stream!");

        return redirect(routes.StreamOutputsController.index(streamId));
    }
}
