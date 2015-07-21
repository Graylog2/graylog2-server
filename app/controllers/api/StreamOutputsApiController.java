package controllers.api;

import controllers.AuthenticatedController;
import lib.json.Json;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.StreamService;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;

import static views.helpers.Permissions.isPermitted;

public class StreamOutputsApiController extends AuthenticatedController {
    private final StreamService streamService;

    @Inject
    public StreamOutputsApiController(StreamService streamService) {
        this.streamService = streamService;
    }

    public Result index(String streamId) throws IOException, APIException {
        if (!isPermitted(RestPermissions.STREAMS_READ, streamId) || !isPermitted(RestPermissions.OUTPUTS_READ))
            return forbidden();

        return ok(Json.toJsonString(streamService.getOutputs(streamId))).as("application/json");
    }

    public Result delete(String streamId, String outputId) throws APIException, IOException {
        if (!isPermitted(RestPermissions.STREAM_OUTPUTS_DELETE, streamId))
            return forbidden();

        streamService.removeOutput(streamId, outputId);
        return ok();
    }

    public Result add(String streamId, String outputId) throws APIException, IOException {
        if (!isPermitted(RestPermissions.STREAM_OUTPUTS_CREATE, streamId))
            return forbidden();

        streamService.addOutput(streamId, outputId);
        return ok();
    }
}
