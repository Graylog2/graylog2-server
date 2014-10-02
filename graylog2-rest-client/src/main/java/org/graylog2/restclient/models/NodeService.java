/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
            radios.put(radio.id, radioFactory.fromSummaryResponse(radio));
        }

        return radios;
    }

    public class NodeNotFoundException extends Exception {
    }
}
