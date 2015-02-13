/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import lib.BreadcrumbList;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.InternalLogger;
import org.graylog2.restclient.models.InternalLoggerSubsystem;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
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
