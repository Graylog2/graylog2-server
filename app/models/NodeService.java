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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.cluster.NodeSummaryResponse;
import models.api.responses.cluster.RadioSummaryResponse;
import models.api.responses.cluster.RadiosResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            r = api.get(NodeSummaryResponse.class)
                    .path("/system/cluster/nodes/{0}", nodeId)
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
        NodeSummaryResponse r = api.get(NodeSummaryResponse.class)
                .path("/system/cluster/node")
                .onlyMasterNode()
                .execute();

        return nodeFactory.fromSummaryResponse(r);
    }

    public Radio loadRadio(String radioId) throws NodeNotFoundException {
        RadioSummaryResponse r;

        try {
            r = api.get(RadioSummaryResponse.class)
                    .path("/system/radios/{0}", radioId)
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

        RadiosResponse r = api.get(RadiosResponse.class).path("/system/radios").execute();
        for (RadioSummaryResponse radio : r.radios) {
            radios.put(radio.id, radioFactory.fromSummaryResponse(radio));
        }

        return radios;
    }

    public class NodeNotFoundException extends Exception {
    }
}
