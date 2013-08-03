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

import lib.APIException;
import lib.Api;
import lib.BreadcrumbList;
import models.Input;
import models.Node;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorsController extends AuthenticatedController {

    public static Result manage(String nodeId, String inputId) {
        try {
            Node node = Node.fromId(nodeId);
            Input input = node.getInput(inputId);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.SystemController.nodes());
            bc.addCrumb(node.getShortNodeId(), routes.SystemController.node(node.getNodeId()));
            bc.addCrumb("Input: " + input.getTitle(), null);
            bc.addCrumb("Extractors", routes.ExtractorsController.manage(nodeId, inputId));

            return ok(views.html.system.inputs.extractors.manage.render(currentUser(), bc, node, input));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

}
