package controllers.api;

import controllers.AuthenticatedController;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Output;
import org.graylog2.restclient.models.OutputService;
import org.graylog2.restclient.models.api.responses.AvailableOutputSummary;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static views.helpers.Permissions.isPermitted;

public class OutputsApiController extends AuthenticatedController {
    private final OutputService outputService;

    @Inject
    public OutputsApiController(OutputService outputService) {
        this.outputService = outputService;
    }

    public Result index() throws APIException, IOException {
        if (!isPermitted(RestPermissions.OUTPUTS_READ)) {
            return forbidden();
        }
        final List<Output> outputs = outputService.list();

        return ok(Json.toJson(outputs));
    }

    public Result available() throws APIException, IOException {
        final Map<String, AvailableOutputSummary> result = outputService.available().types;
        return ok(Json.toJson(result));
    }

    public Result delete(String outputId) throws APIException, IOException {
        if (!isPermitted(RestPermissions.OUTPUTS_TERMINATE, outputId))
            return forbidden();

        outputService.delete(outputId);
        return ok();
    }
}
