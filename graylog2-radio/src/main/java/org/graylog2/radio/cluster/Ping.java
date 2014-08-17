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
package org.graylog2.radio.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Ping {

    /*
     * This is extremely simple. Once we do more than just the ping API calls
     * we should build something proper here.
     */

    private static final Logger LOG = LoggerFactory.getLogger(Ping.class);

    public static void ping(AsyncHttpClient client, URI server, URI ourUri, String radioId) throws IOException, ExecutionException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("rest_transport_address", ourUri.toString());

        final UriBuilder uriBuilder = UriBuilder.fromUri(server);
        uriBuilder.path("/system/radios/" + radioId + "/ping");

        Future<Response> f = client.preparePut(uriBuilder.build().toString())
                .setBody(rootNode.toString())
                .execute();

        Response r = f.get();

        if (r.getStatusCode() != 200) {
            throw new RuntimeException("Expected ping HTTP response [200] but got [" + r.getStatusCode() + "].");
        }
    }

    public static class Pinger implements Runnable {

        private final AsyncHttpClient httpClient;
        private final String nodeId;
        private final URI serverUri;
        private final URI ourUri;

        @Inject
        public Pinger(AsyncHttpClient httpClient, @Named("OurRadioUri") URI ourUri, @Named("ServerUri") URI serverUri, ServerStatus serverStatus) {
            this.httpClient = httpClient;
            this.nodeId = serverStatus.getNodeId().toString();
            this.ourUri = ourUri;
            this.serverUri = serverUri;
        }

        @Override
        public void run() {
            ping();
        }

        public void ping() {
            LOG.debug("Updating (ping) this radio instance [{}] in the Graylog2 cluster at [{}]", nodeId, serverUri);
            try {
                Ping.ping(httpClient, serverUri, ourUri, nodeId);
            } catch (Exception e) {
                LOG.error("Cluster ping failed.", e);
            }
        }

    }

}
