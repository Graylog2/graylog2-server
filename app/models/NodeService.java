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

package models;

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.ServerNodes;
import models.api.responses.NodeSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NodeService {
    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final ApiClient api;
    private final Node.Factory nodeFactory;

    @Inject
    public NodeService(ApiClient api, Node.Factory nodeFactory) {
        this.api = api;
        this.nodeFactory = nodeFactory;
    }

    public Node loadNode(String nodeId) {
        NodeSummaryResponse r;
        try {
            r = api.get(NodeSummaryResponse.class)
                    .node(ServerNodes.any())
                    .path("/system/cluster/nodes/{0}", nodeId)
                    .execute();
            return nodeFactory.fromSummaryResponse(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }

}
