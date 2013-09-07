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
import models.*;
import play.mvc.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemController extends AuthenticatedController {

    public static Result index(Integer page) {
        try {
            List<Notification> notifications = Notification.all();
            List<SystemJob> systemJobs = SystemJob.all();
            int totalSystemMessages = SystemMessage.total();
            List<SystemMessage> systemMessages = SystemMessage.all(Integer.valueOf(page-1));
            ESClusterHealth clusterHealth = ESClusterHealth.get();

            return ok(views.html.system.index.render(
                    currentUser(),
                    systemJobs,
                    clusterHealth,
                    systemMessages,
                    totalSystemMessages,
                    page,
                    notifications
            ));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public static Result nodes() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Nodes", routes.SystemController.nodes());

        try {
            List<ServerJVMStats> serverJvmStats = ServerJVMStats.get();
            Map<String, Node> nodes = Node.asMap();
            Map<String, BufferInfo> bufferInfo = Maps.newHashMap();

            // Ask every node for buffer info.
            for(Node node : nodes.values()) {
                bufferInfo.put(node.getNodeId(), BufferInfo.ofNode(node));
            }

            return ok(views.html.system.nodes.render(currentUser(), bc, serverJvmStats, nodes, bufferInfo));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public static Result node(String nodeId) {
        // TODO
        return ok("implement me");
    }

    public static Result threadDump(String nodeId) {
        try {
            Node node = Node.fromId(nodeId);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.SystemController.nodes());
            bc.addCrumb(node.getShortNodeId(), routes.SystemController.node(node.getNodeId()));
            bc.addCrumb("Thread dump", routes.SystemController.threadDump(nodeId));

            return ok(views.html.system.threaddump.render(currentUser(), bc, node, node.getThreadDump()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }


}
