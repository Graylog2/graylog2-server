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
import lib.APIException;
import lib.ApiClient;
import models.alerts.Alert;
import models.alerts.AlertCondition;
import models.alerts.AlertConditionService;
import models.Stream;
import models.StreamService;
import models.api.requests.alerts.CreateAlertConditionRequest;
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
    private AlertConditionService alertConditionService;

    public Result index(String streamId) {
        try {
            Stream stream = streamService.get(streamId);
            List<AlertCondition> alertConditions = alertConditionService.allOfStream(stream);
            List<Alert> alerts = stream.getAlerts();
            long totalAlerts = stream.getTotalAlerts();

            return ok(views.html.alerts.manage.render(currentUser(), stream, alertConditions, totalAlerts, alerts));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch stream. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result addTypeMessageCount(String streamId) {
        Map<String,String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

        if(!checkParam("grace", form) || !checkParam("time", form)
                || !checkParam("threshold", form)
                || !checkParam("threshold_type", form)) {
            flash("error", "Could not add alert condition: Missing parameters.");
            return redirect(routes.AlertsController.index(streamId));
        }

        try {
            Stream stream = streamService.get(streamId);

            CreateAlertConditionRequest request = new CreateAlertConditionRequest();
            request.creatorUserId = currentUser().getName();
            request.type = "message_count";
            request.parameters.put("grace", Integer.parseInt(form.get("grace")));
            request.parameters.put("time", Integer.parseInt(form.get("time")));
            request.parameters.put("threshold", Integer.parseInt(form.get("threshold")));
            request.parameters.put("threshold_type", form.get("threshold_type"));

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

    public Result addTypeFieldValue(String streamId) {
        Map<String,String> form = flattenFormUrlEncoded(request().body().asFormUrlEncoded());

        if(!checkParam("grace", form) || !checkParam("time", form)
                || !checkParam("threshold", form)
                || !checkParam("threshold_type", form)
                || !checkParam("field", form)
                || !checkParam("type", form)) {
            flash("error", "Could not add alert condition: Missing parameters.");
            return redirect(routes.AlertsController.index(streamId));
        }

        try {
            Stream stream = streamService.get(streamId);

            CreateAlertConditionRequest request = new CreateAlertConditionRequest();
            request.creatorUserId = currentUser().getName();
            request.type = "field_value";
            request.parameters.put("grace", Integer.parseInt(form.get("grace")));
            request.parameters.put("time", Integer.parseInt(form.get("time")));
            request.parameters.put("threshold", Integer.parseInt(form.get("threshold")));
            request.parameters.put("threshold_type", form.get("threshold_type"));
            request.parameters.put("type", form.get("type"));
            request.parameters.put("field", form.get("field"));

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

    private boolean checkParam(String key, Map<String,String> form) {
        return form.containsKey(key) && !form.get(key).isEmpty();
    }

}