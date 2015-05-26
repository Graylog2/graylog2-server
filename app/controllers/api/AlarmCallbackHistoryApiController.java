package controllers.api;

import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.AlarmCallbackHistoryService;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;

public class AlarmCallbackHistoryApiController extends AuthenticatedController {
    private final AlarmCallbackHistoryService alarmCallbackHistoryService;

    @Inject
    public AlarmCallbackHistoryApiController(AlarmCallbackHistoryService alarmCallbackHistoryService) {
        this.alarmCallbackHistoryService = alarmCallbackHistoryService;
    }

    public Result list(String streamId, String alertId) throws APIException, IOException {
        return ok(Json.toJson(alarmCallbackHistoryService.list(streamId, alertId)));
    }
}
