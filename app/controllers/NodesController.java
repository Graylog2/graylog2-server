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
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.BreadcrumbList;
import lib.ServerNodes;
import models.*;
import play.mvc.Result;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lib.security.RestPermissions.BUFFERS_READ;
import static lib.security.RestPermissions.JVMSTATS_READ;
import static views.helpers.Permissions.isPermitted;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NodesController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private ServerNodes serverNodes;

    public Result nodes() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Nodes", routes.NodesController.nodes());

        List<NodeJVMStats> serverJvmStats = isPermitted(JVMSTATS_READ) ? clusterService.getClusterJvmStats() : Collections.<NodeJVMStats>emptyList();
        Map<String, Node> nodes = serverNodes.asMap();
        Map<String, BufferInfo> bufferInfo = Maps.newHashMap();

        if (isPermitted(BUFFERS_READ)) {
            // Ask every node for buffer info.
            for(Node node : nodes.values()) {
                bufferInfo.put(node.getNodeId(), node.getBufferInfo());
            }
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
            bc.addCrumb("Nodes", routes.NodesController.nodes());
            bc.addCrumb(node.getShortNodeId(), routes.NodesController.node(node.getNodeId()));

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
            bc.addCrumb("Nodes", routes.NodesController.nodes());
            bc.addCrumb(node.getShortNodeId(), routes.NodesController.node(node.getNodeId()));
            bc.addCrumb("Thread dump", routes.NodesController.threadDump(nodeId));

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

    public Result pauseMessageProcessing(String nodeId) {
        try {
            final Node node = nodeService.loadNode(nodeId);
            node.pause();
            return redirect(routes.NodesController.node(nodeId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not set message processing state. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result resumeMessageProcessing(String nodeId) {
        try {
            final Node node = nodeService.loadNode(nodeId);
            node.resume();
            return redirect(routes.NodesController.node(nodeId));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not set message processing state. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result overrideLbStatus(String nodeId, String override) {
        try {
            final Node node = nodeService.loadNode(nodeId);
            node.overrideLbStatus(override);
            return redirect(routes.NodesController.nodes());
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not set load balancer state. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result shutdown(String nodeId) {
        try {
            final Node node = nodeService.loadNode(nodeId);
            node.shutdown();
            return redirect(routes.NodesController.nodes());
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Failed to shut down node. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

}
