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
import models.*;
import play.mvc.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private ServerNodes serverNodes;

    public Result index(Integer page) {
        try {
            List<Notification> notifications = clusterService.allNotifications();
            List<SystemJob> systemJobs = clusterService.allSystemJobs();
            int totalSystemMessages = clusterService.getNumberOfSystemMessages();
            List<SystemMessage> systemMessages = clusterService.getSystemMessages(page - 1);
            ESClusterHealth clusterHealth = clusterService.getESClusterHealth();

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
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result nodes() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Nodes", routes.SystemController.nodes());

        List<NodeJVMStats> serverJvmStats = clusterService.getClusterJvmStats();
        Map<String, Node> nodes = serverNodes.asMap();
        Map<String, BufferInfo> bufferInfo = Maps.newHashMap();

        // Ask every node for buffer info.
        for(Node node : nodes.values()) {
            bufferInfo.put(node.getNodeId(), node.getBufferInfo());
        }

        Map<String, Radio> radios = null;
        try {
            radios = nodeService.radios();
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch radio information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        return ok(views.html.system.nodes.index.render(currentUser(), bc, serverJvmStats, nodes, radios, bufferInfo));
    }

    public Result node(String nodeId) {
        try {
            Node node = nodeService.loadNode(nodeId);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.SystemController.nodes());

            return ok(views.html.system.nodes.show.render(currentUser(), bc, node));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result threadDump(String nodeId) {
        try {
            Node node = nodeService.loadNode(nodeId);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.SystemController.nodes());
            bc.addCrumb(node.getShortNodeId(), routes.SystemController.node(node.getNodeId()));
            bc.addCrumb("Thread dump", routes.SystemController.threadDump(nodeId));

            return ok(views.html.system.threaddump.render(currentUser(), bc, node, node.getThreadDump()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result threadDumpRadio(String radioId) {
        try {
            Radio radio = nodeService.loadRadio(radioId);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.SystemController.nodes());
            bc.addCrumb(radio.getShortNodeId(), routes.RadiosController.show(radio.getId()));
            bc.addCrumb("Thread dump", routes.SystemController.threadDumpRadio(radioId));

            return ok(views.html.system.threaddump.render(currentUser(), bc, radio, radio.getThreadDump()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }



}
