package controllers.api;

import controllers.AuthenticatedController;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbackSummaryResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.AlarmCallback;
import org.graylog2.restclient.models.AlarmCallbackService;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
}
