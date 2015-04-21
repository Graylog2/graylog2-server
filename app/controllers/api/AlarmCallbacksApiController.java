package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.AuthenticatedController;
import lib.security.RestPermissions;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbackSummaryResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.AlarmCallback;
import org.graylog2.restclient.models.AlarmCallbackService;
import org.graylog2.restclient.models.api.requests.alarmcallbacks.CreateAlarmCallbackRequest;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static views.helpers.Permissions.isPermitted;

public class AlarmCallbacksApiController extends AuthenticatedController {
    private final AlarmCallbackService alarmCallbackService;

    @Inject
    public AlarmCallbacksApiController(AlarmCallbackService alarmCallbackService) {
        this.alarmCallbackService = alarmCallbackService;
    }

    public Result available(String streamId) throws IOException, APIException {
        Map<String, AvailableAlarmCallbackSummaryResponse> availableAlarmCallbacks = alarmCallbackService.available(streamId);

        return ok(Json.toJson(availableAlarmCallbacks));
    }

    public Result list(String streamId) throws IOException, APIException {
        final List<AlarmCallback> alarmCallbacks = this.alarmCallbackService.all(streamId);

        return ok(Json.toJson(alarmCallbacks));
    }

    public Result create(String streamId) throws IOException, APIException {
        if (!isPermitted(RestPermissions.STREAMS_EDIT, streamId))
            return forbidden();

        final JsonNode json = request().body().asJson();
        final CreateAlarmCallbackRequest request = Json.fromJson(json, CreateAlarmCallbackRequest.class);

        return ok(Json.toJson(alarmCallbackService.create(streamId, request)));
    }

    public Result delete(String streamId, String alarmCallbackId) throws IOException, APIException {
        if (!isPermitted(RestPermissions.STREAMS_EDIT, streamId))
            return forbidden();

        alarmCallbackService.delete(streamId, alarmCallbackId);

        return ok();
    }
}
