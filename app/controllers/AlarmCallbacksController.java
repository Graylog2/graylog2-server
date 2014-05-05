package controllers;

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.AlarmCallback;
import org.graylog2.restclient.models.AlarmCallbackService;
import org.graylog2.restclient.models.api.requests.alarmcallbacks.CreateAlarmCallbackRequest;
import play.data.Form;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;

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

        request.creatorUserId = currentUser().getName();

        alarmCallbackService.create(streamId, request);

        return redirect(routes.AlertsController.index(streamId));
    }

    public Result delete(String streamId, String alarmCallbackId) throws IOException, APIException {
        alarmCallbackService.delete(streamId, alarmCallbackId);

        return redirect(routes.AlertsController.index(streamId));
    }
}
