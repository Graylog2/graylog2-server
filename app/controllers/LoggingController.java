/*
 * Copyright 2013 TORCH UG
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
 */
package controllers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.BreadcrumbList;
import lib.ServerNodes;
import models.InternalLogger;
import models.InternalLoggerSubsystem;
import models.Node;
import models.NodeService;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class LoggingController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;
    @Inject
    private ServerNodes serverNodes;

    public Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Logging", routes.LoggingController.index());

        Map<Node, List<InternalLogger>> loggers = Maps.newHashMap();
        Map<Node, Map<String, InternalLoggerSubsystem>> subsystems = Maps.newHashMap();
        for (Node node : serverNodes.all()) {
            loggers.put(node, node.allLoggers());
            subsystems.put(node, node.allLoggerSubsystems());
        }

        return ok(views.html.system.logging.index.render(currentUser(), bc, loggers, subsystems));
    }

    public Result setSubsystemLevel(String nodeId, String subsystem, String level) {
        try {
            Node node = nodeService.loadNode(nodeId);
            node.setSubsystemLoggerLevel(subsystem, level);

            return redirect(routes.LoggingController.index());
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not set log level. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

}