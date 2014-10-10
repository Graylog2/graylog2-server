package controllers;

import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.AlarmCallbackService;
import org.graylog2.restclient.models.api.requests.alarmcallbacks.CreateAlarmCallbackRequest;
import org.graylog2.restclient.models.api.responses.alarmcallbacks.GetSingleAvailableAlarmCallbackResponse;
import play.data.Form;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbacksController extends AuthenticatedController {
    private static final Form<CreateAlarmCallbackRequest> alarmCallbackForm = Form.form(CreateAlarmCallbackRequest.class);
    private final AlarmCallbackService alarmCallbackService;

    @Inject
    public AlarmCallbacksController(AlarmCallbackService alarmCallbackService) {
        this.alarmCallbackService = alarmCallbackService;
    }

    public Result create(String streamId) throws IOException, APIException {
        Form<CreateAlarmCallbackRequest> boundForm = alarmCallbackForm.bindFromRequest();
        CreateAlarmCallbackRequest request = boundForm.get();

        Map<String, GetSingleAvailableAlarmCallbackResponse> availableAlarmCallbacks = alarmCallbackService.available(streamId);

        request.configuration = extractConfiguration(request.configuration, availableAlarmCallbacks.get(request.type));

        alarmCallbackService.create(streamId, request);

        return redirect(routes.AlertsController.index(streamId));
    }

    public Result delete(String streamId, String alarmCallbackId) throws IOException, APIException {
        alarmCallbackService.delete(streamId, alarmCallbackId);

        return redirect(routes.AlertsController.index(streamId));
    }

    protected Map<String, Object> extractConfiguration(Map<String, Object> form, GetSingleAvailableAlarmCallbackResponse alarmCallbackInfo) {
        Map<String, Object> configuration = Maps.newHashMap();
        for (Map.Entry<String, Object> entry : form.entrySet()) {
            Object value;
            // Decide what to cast to. (string, bool, number)
            switch(alarmCallbackInfo.requested_configuration.get(entry.getKey()).get("type").toString()) {
                case "text":
                    value = entry.getValue().toString();
                    break;
                case "number":
                    value = Integer.parseInt(entry.getValue().toString());
                    break;
                case "boolean":
                    value = entry.getValue().toString().equals("true");
                    break;
                case "dropdown":
                    value = entry.getValue().toString();
                    break;
                default:
                    value = entry.getValue();
            }

            configuration.put(entry.getKey(), value);
        }

        return configuration;
    }
}
