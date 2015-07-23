package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import controllers.AuthenticatedController;
import lib.json.Json;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Output;
import org.graylog2.restclient.models.OutputService;
import org.graylog2.restclient.models.api.requests.outputs.OutputLaunchRequest;
import org.graylog2.restclient.models.api.responses.AvailableOutputSummary;
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

        return ok(Json.toJsonString(outputs)).as(MediaType.JSON_UTF_8.toString());
    }

    public Result available(String outputType) throws APIException, IOException {
        final Map<String, AvailableOutputSummary> types = outputService.available().types;
        final AvailableOutputSummary result = types.get(outputType);
        if (result != null) {
            return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
        } else {
            return notFound();
        }
    }

    public Result availableTypes() throws APIException, IOException {
        final Map<String, AvailableOutputSummary> types = outputService.available().types;

        final Map<String, String> result = Maps.newHashMap();

        for (Map.Entry<String, AvailableOutputSummary> entry : types.entrySet()) {
            result.put(entry.getKey(), entry.getValue().name);
        }
        return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
    }

    public Result delete(String outputId) throws APIException, IOException {
        if (!isPermitted(RestPermissions.OUTPUTS_TERMINATE, outputId))
            return forbidden();

        outputService.delete(outputId);
        return ok();
    }

    public Result create() throws APIException, IOException {
        if (!isPermitted(RestPermissions.OUTPUTS_CREATE))
            return forbidden();

        final JsonNode json = request().body().asJson();
        final OutputLaunchRequest request = Json.fromJson(json, OutputLaunchRequest.class);

        final Output output = outputService.create(request);

        return ok(Json.toJsonString(output)).as(MediaType.JSON_UTF_8.toString());
    }

    public Result update(String outputId) throws APIException, IOException {
        if (!isPermitted(RestPermissions.OUTPUTS_EDIT, outputId))
            return forbidden();

        final JsonNode json = request().body().asJson();
        final OutputLaunchRequest request = Json.fromJson(json, OutputLaunchRequest.class);

        final Output output = outputService.update(outputId, request);

        return ok(Json.toJsonString(output)).as(MediaType.JSON_UTF_8.toString());
    }
}
