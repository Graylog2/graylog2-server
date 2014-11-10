/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package controllers;

import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.*;
import org.graylog2.restclient.models.alerts.Alert;
import org.graylog2.restclient.models.alerts.AlertCondition;
import org.graylog2.restclient.models.alerts.AlertConditionService;
import org.graylog2.restclient.models.api.requests.alerts.CreateAlertConditionRequest;
import org.graylog2.restclient.models.api.responses.alarmcallbacks.GetSingleAvailableAlarmCallbackResponse;
import play.mvc.BodyParser;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertsController extends AuthenticatedController {

    @Inject
    private StreamService streamService;

    @Inject
    private UserService userService;

    @Inject
    private AlertConditionService alertConditionService;

    @Inject
    private AlarmCallbackService alarmCallbackService;

    @Inject
    private NodeService nodeService;

    public Result index(String streamId) {
        try {
            Stream stream = streamService.get(streamId);
            List<AlertCondition> alertConditions = alertConditionService.allOfStream(stream);
            List<Alert> alerts = stream.getAlerts();
            long totalAlerts = stream.getTotalAlerts();

            StringBuilder users = new StringBuilder();
            users.append("[");
            List<User> userList = userService.allExceptAdmin();
            int i = 0;
            for(User user : userList){
                users.append("\"").append(user.getName()).append("\"");

                if(i != userList.size()-1) {
                    users.append(",");
                }
                i++;
            }
            users.append("]");

            Map<String, GetSingleAvailableAlarmCallbackResponse> availableAlarmCallbacks = alarmCallbackService.available(streamId);
            List<AlarmCallback> alarmCallbacks = alarmCallbackService.all(streamId);

            return ok(views.html.alerts.manage.render(
                    currentUser(),
                    stream,
                    alertConditions,
                    totalAlerts,
                    alerts,
                    users.toString(),
                    availableAlarmCallbacks,
                    alarmCallbacks,
                    nodeService.loadMasterNode()
            ));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch stream. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result addTypeMessageCount(String streamId) {
        Map<String,String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

        if(!checkParam("grace", form) || !checkParam("time", form)
                || !checkParam("threshold", form)
                || !checkParam("threshold_type", form)
                || !checkParam("backlog", form)) {
            flash("error", "Could not add alert condition: Missing parameters.");
            return redirect(routes.AlertsController.index(streamId));
        }

        try {
            Stream stream = streamService.get(streamId);

            CreateAlertConditionRequest request = new CreateAlertConditionRequest();
            request.type = "message_count";
            request.parameters.put("grace", Integer.parseInt(form.get("grace")));
            request.parameters.put("time", Integer.parseInt(form.get("time")));
            request.parameters.put("threshold", Integer.parseInt(form.get("threshold")));
            request.parameters.put("threshold_type", form.get("threshold_type"));
            request.parameters.put("backlog", Integer.parseInt(form.get("backlog")));

            stream.addAlertCondition(request);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not create alert condition. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        flash("success", "Added alert condition.");
        return redirect(routes.AlertsController.index(streamId));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result addTypeFieldValue(String streamId) {
        Map<String,String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

        if(!checkParam("grace", form) || !checkParam("time", form)
                || !checkParam("threshold", form)
                || !checkParam("threshold_type", form)
                || !checkParam("field", form)
                || !checkParam("type", form)
                || !checkParam("backlog", form)) {
            flash("error", "Could not add alert condition: Missing parameters.");
            return redirect(routes.AlertsController.index(streamId));
        }

        try {
            Stream stream = streamService.get(streamId);

            CreateAlertConditionRequest request = new CreateAlertConditionRequest();
            request.type = "field_value";
            request.parameters.put("grace", Integer.parseInt(form.get("grace")));
            request.parameters.put("time", Integer.parseInt(form.get("time")));
            request.parameters.put("threshold", Double.parseDouble(form.get("threshold")));
            request.parameters.put("threshold_type", form.get("threshold_type"));
            request.parameters.put("type", form.get("type"));
            request.parameters.put("field", form.get("field"));
            request.parameters.put("backlog", Integer.parseInt(form.get("backlog")));

            stream.addAlertCondition(request);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not create alert condition. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        flash("success", "Added alert condition.");
        return redirect(routes.AlertsController.index(streamId));
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result updateCondition(String streamId, String conditionId) {
        Map<String,String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

        for (String key : form.keySet()) {
            if (!checkParam(key, form)) {
                flash("error", "Could not add alert condition: Missing parameters.");
                return redirect(routes.AlertsController.index(streamId));
            }
        }

        try {
            Stream stream = streamService.get(streamId);

            CreateAlertConditionRequest request = new CreateAlertConditionRequest();

            for (Map.Entry<String, String> entry : form.entrySet()) {
                try {
                    request.parameters.put(entry.getKey(), Integer.parseInt(entry.getValue()));
                } catch (Exception e1) {
                    try {
                        request.parameters.put(entry.getKey(), Double.parseDouble(entry.getValue()));
                    } catch (Exception e2) {
                        request.parameters.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            alertConditionService.update(stream, conditionId, request);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not create alert condition. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        flash("success", "Updated alert condition.");
        return redirect(routes.AlertsController.index(streamId));
    }

    public Result removeCondition(String streamId, String conditionId) {
        try {
            Stream stream = streamService.get(streamId);
            alertConditionService.delete(stream, conditionId);

            flash("success", "Deleted alert condition.");
            return redirect(routes.AlertsController.index(streamId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch stream. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result addReceiverUser(String streamId) {
        Map<String,String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());
        String username = form.get("username");

        if (username == null || username.trim().isEmpty()) {
            flash("error", "No username provided.");
            return redirect(routes.AlertsController.index(streamId));
        }

        try {
            User receiver = userService.load(username);

            if (receiver == null) {
                flash("error", "Could not add alert receiver: Unknown user.");
                return redirect(routes.AlertsController.index(streamId));
            }

            Stream stream = streamService.get(streamId);
            stream.addAlertReceiver(receiver);

            flash("success", "Added alert receiver.");
            return redirect(routes.AlertsController.index(streamId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not add alert receiver: We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result removeReceiverUser(String streamId, String username) {
        if (username == null || username.trim().isEmpty()) {
            flash("error", "No username provided.");
            return redirect(routes.AlertsController.index(streamId));
        }

        try {
            User receiver = userService.load(username);

            if (receiver == null) {
                flash("error", "Could not remove alert receiver: Unknown user.");
                return redirect(routes.AlertsController.index(streamId));
            }

            Stream stream = streamService.get(streamId);
            stream.removeAlertReceiver(receiver);

            flash("success", "Removed alert receiver.");
            return redirect(routes.AlertsController.index(streamId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not add alert receiver: We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result addReceiverEmail(String streamId) {
        Map<String,String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());
        String email = form.get("email");

        try {
            Stream stream = streamService.get(streamId);
            stream.addAlertReceiver(email);

            flash("success", "Added alert receiver.");
            return redirect(routes.AlertsController.index(streamId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not add alert receiver: We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result removeReceiverEmail(String streamId, String email) {
        try {
            Stream stream = streamService.get(streamId);
            stream.removeAlertReceiver(email);

            flash("success", "Removed alert receiver.");
            return redirect(routes.AlertsController.index(streamId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not add alert receiver: We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result sendDummyAlert(String streamId) {
        try {
            streamService.sendDummyAlert(streamId);
            flash("success", "Sent dummy alert to all subscribers.");
            return redirect(routes.AlertsController.index(streamId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            flash("error", "Unable to send dummy alert, check server log for details: " + e.getMessage());
            return redirect(routes.AlertsController.index(streamId));
        }
    }

    private boolean checkParam(String key, Map<String,String> form) {
        return form.containsKey(key) && !form.get(key).isEmpty();
    }

}
