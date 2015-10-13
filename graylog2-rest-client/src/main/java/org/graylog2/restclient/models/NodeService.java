/**
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
package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.cluster.NodeSummaryResponse;
import org.graylog2.restroutes.generated.routes;

import javax.inject.Inject;
import java.io.IOException;

public class NodeService {
    private final ApiClient api;
    private final Node.Factory nodeFactory;

    @Inject
    public NodeService(ApiClient api, Node.Factory nodeFactory) {
        this.api = api;
        this.nodeFactory = nodeFactory;
    }

    public Node loadNode(String nodeId) throws NodeNotFoundException {
        NodeSummaryResponse r;
        try {
            r = api.path(routes.ClusterResource().node(nodeId), NodeSummaryResponse.class)
                    .execute();
            return nodeFactory.fromSummaryResponse(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (APIException e) {
            if (e.getHttpCode() == 404) {
                throw new NodeNotFoundException();
            }


            throw new RuntimeException(e);
        }
    }

    public Node loadMasterNode() throws APIException, IOException {
        NodeSummaryResponse r = api.path(routes.ClusterResource().node(), NodeSummaryResponse.class)
                .onlyMasterNode()
                .execute();

        return nodeFactory.fromSummaryResponse(r);
    }

    public static class NodeNotFoundException extends Exception {
    }
}
