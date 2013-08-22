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
import com.google.gson.Gson;
import lib.APIException;
import lib.Api;
import lib.BreadcrumbList;
import models.Extractor;
import models.Input;
import models.Node;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorsController extends AuthenticatedController {

    public static Result manage(String nodeId, String inputId) {
        try {
            Node node = Node.fromId(nodeId);
            Input input = node.getInput(inputId);

            return ok(views.html.system.inputs.extractors.manage.render(
                    currentUser(),
                    standardBreadcrumbs(node, input),
                    node,
                    input,
                    Extractor.all(node, input))
            );
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    public static Result newExtractor(String nodeId, String inputId, String extractorType, String field, String example) {
        try {
            Node node = Node.fromId(nodeId);
            Input input = node.getInput(inputId);

            return ok(views.html.system.inputs.extractors.new_extractor.render(
                    currentUser(),
                    standardBreadcrumbs(node, input),
                    node,
                    input,
                    Extractor.Type.valueOf(extractorType.toUpperCase()),
                    field,
                    example)
            );
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    public static Result create(String nodeId, String inputId) {
        Map<String, String[]> form = request().body().asFormUrlEncoded();
        Extractor.Type extractorType = Extractor.Type.valueOf(form.get("extractor_type")[0].toUpperCase());
        Node node = Node.fromId(nodeId);

        Extractor extractor;
        try {
            try {
                extractor = new Extractor(
                        Extractor.CursorStrategy.valueOf(form.get("cut_or_copy")[0].toUpperCase()),
                        form.get("title")[0],
                        form.get("source_field")[0],
                        form.get("target_field")[0],
                        extractorType,
                        currentUser(),
                        Extractor.ConditionType.valueOf(form.get("condition_type")[0].toUpperCase()),
                        form.get("condition_value")[0]
                );
            } catch (NullPointerException e) {
                Logger.error("Cannot build extractor configuration.", e);
                return badRequest();
            }

            extractor.loadConfigFromForm(extractorType, form);
            extractor.loadConvertersFromForm(form);
            extractor.create(node, node.getInput(inputId));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not create extractor! We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }

        return redirect(routes.ExtractorsController.manage(nodeId, inputId));
    }

    public static Result delete(String nodeId, String inputId, String extractorId) {
        try {
            Node node = Node.fromId(nodeId);
            Extractor.delete(node, node.getInput(inputId), extractorId);

            return redirect(routes.ExtractorsController.manage(nodeId, inputId));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not delete extractor! We expected HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    private static BreadcrumbList standardBreadcrumbs(Node node, Input input) {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Nodes", routes.SystemController.nodes());
        bc.addCrumb(node.getShortNodeId(), routes.SystemController.node(node.getNodeId()));
        bc.addCrumb("Input: " + input.getTitle(), null);
        bc.addCrumb("Extractors", routes.ExtractorsController.manage(node.getNodeId(), input.getId()));

        return bc;
    }

}
