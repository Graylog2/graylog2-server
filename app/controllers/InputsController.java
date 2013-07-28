/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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

import com.google.common.collect.Maps;
import lib.APIException;
import lib.Api;
import lib.BreadcrumbList;
import lib.plugin.configuration.BooleanField;
import models.BufferInfo;
import models.Input;
import models.Node;
import models.ServerJVMStats;
import models.api.responses.InputTypeResponse;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InputsController extends AuthenticatedController {

    public static Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Inputs", routes.InputsController.index());

        try {
            return ok(views.html.system.inputs.index.render(currentUser(), bc, Node.all()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch nodes. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public static Result manage(String nodeId) {
        Node node = Node.fromId(nodeId);

        if (node == null) {
            String message = "Did not find node.";
            return status(404, views.html.errors.error.render(message, new RuntimeException(), request()));
        }

        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Inputs", routes.InputsController.index());
        bc.addCrumb(node.getNodeId(), routes.InputsController.manage(node.getNodeId()));

        try {
            return ok(views.html.system.inputs.manage.render(
                    currentUser(),
                    bc,
                    node,
                    Input.getAllTypeInformation(node)
            ));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public static Result launch(String nodeId) {
        Map<String, Object> configuration = Maps.newHashMap();
        Map<String, String[]> form = request().body().asFormUrlEncoded();

        String inputType = form.get("type")[0];
        String inputTitle = form.get("title")[0];

        try {
            InputTypeResponse inputInfo = Input.getTypeInformation(Node.fromId(nodeId), inputType);

            for (Map.Entry<String, String[]> f : form.entrySet()) {
                if (!f.getKey().startsWith("configuration_")) {
                    continue;
                }

                String key = f.getKey().substring("configuration_".length());
                Object value;

                if (f.getValue().length > 0) {
                    String stringValue = f.getValue()[0];

                    // Decide what to cast to. (string, bool, number)
                    switch((String) inputInfo.requestedConfiguration.get(key).get("type")) {
                        case "text":
                            value = stringValue;
                            break;
                        case "number":
                            value = Integer.parseInt(stringValue);
                            break;
                        case "boolean":
                            value = stringValue.equals("true");
                            break;
                        default: continue;
                    }

                } else {
                    continue;
                }

                configuration.put(key, value);
            }


            Input.launch(Node.fromId(nodeId), inputTitle, inputType, configuration, currentUser().getId());

            return redirect(routes.InputsController.manage(nodeId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 202, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

}
