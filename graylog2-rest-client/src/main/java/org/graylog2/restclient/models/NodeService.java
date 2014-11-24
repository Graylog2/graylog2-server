/**
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
package org.graylog2.restclient.models;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.cluster.NodeSummaryResponse;
import org.graylog2.restclient.models.api.responses.cluster.RadioSummaryResponse;
import org.graylog2.restclient.models.api.responses.cluster.RadiosResponse;
import org.graylog2.restroutes.generated.routes;

import java.io.IOException;
import java.util.Map;

public class NodeService {
    private final ApiClient api;
    private final Node.Factory nodeFactory;
    private final Radio.Factory radioFactory;

    @Inject
    public NodeService(ApiClient api, Node.Factory nodeFactory, Radio.Factory radioFactory) {
        this.api = api;
        this.nodeFactory = nodeFactory;
        this.radioFactory = radioFactory;
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

    public Radio loadRadio(String radioId) throws NodeNotFoundException {
        RadioSummaryResponse r;

        try {
            r = api.path(routes.RadiosResource().radio(radioId), RadioSummaryResponse.class)
                    .execute();
            return radioFactory.fromSummaryResponse(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (APIException e) {
            if (e.getHttpCode() == 404) {
                throw new NodeNotFoundException();
            }

            throw new RuntimeException(e);
        }
    }

    public Map<String, Radio> radios() throws APIException, IOException {
        Map<String, Radio> radios = Maps.newHashMap();

        RadiosResponse r = api.path(routes.RadiosResource().radios(), RadiosResponse.class).execute();
        for (RadioSummaryResponse radio : r.radios) {
            radios.put(radio.nodeId, radioFactory.fromSummaryResponse(radio));
        }

        return radios;
    }

    public static class NodeNotFoundException extends Exception {
    }
}
