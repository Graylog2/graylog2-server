/**
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
 *
 */
package controllers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.*;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lib.security.RestPermissions.BUFFERS_READ;
import static lib.security.RestPermissions.JVMSTATS_READ;
import static views.helpers.Permissions.isPermitted;

public class NodesController extends AuthenticatedController {

    private final NodeService nodeService;
    private final ClusterService clusterService;
    private final ServerNodes serverNodes;
    private final PluginService pluginService;

    @Inject
    public NodesController(NodeService nodeService, ClusterService clusterService, ServerNodes serverNodes, PluginService pluginService) {
        this.nodeService = nodeService;
        this.clusterService = clusterService;
        this.serverNodes = serverNodes;
        this.pluginService = pluginService;
    }

    public Result nodes() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Nodes", routes.NodesController.nodes());

        List<NodeJVMStats> serverJvmStats = isPermitted(JVMSTATS_READ) ? clusterService.getClusterJvmStats() : Collections.<NodeJVMStats>emptyList();
        Map<String, Node> nodes = serverNodes.asMap();
        Map<String, Node> updatedNodes = Maps.newHashMap();

        for (String nodeId : nodes.keySet()) {
            try {
                updatedNodes.put(nodeId, nodeService.loadNode(nodeId));
            } catch (NodeService.NodeNotFoundException e) {
                Logger.error("Could not load node information", e);
            }
        }

        Map<String, BufferInfo> bufferInfo = Maps.newHashMap();

        if (isPermitted(BUFFERS_READ)) {
            // Ask every node for buffer info.
            for(Node node : updatedNodes.values()) {
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

        return ok(views.html.system.nodes.index.render(currentUser(), bc, serverJvmStats, updatedNodes, radios, bufferInfo));
    }

    public Result node(String nodeId) {
        try {
            Node node = nodeService.loadNode(nodeId);

            List<Plugin> installedPlugins = pluginService.list(node);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.NodesController.nodes());
            bc.addCrumb(node.getShortNodeId(), routes.NodesController.node(node.getNodeId()));

            return ok(views.html.system.nodes.show.render(currentUser(), bc, node, installedPlugins));
        } catch (NodeService.NodeNotFoundException e) {
            flash("error", "Could not find node '" + nodeId + "'");
            return redirect(routes.NodesController.nodes());
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch node information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
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
