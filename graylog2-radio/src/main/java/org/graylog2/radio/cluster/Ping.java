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
package org.graylog2.radio.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import javax.inject.Named;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.rest.models.radio.requests.PingRequest;
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
public class Ping implements Runnable {

    /*
     * This is extremely simple. Once we do more than just the ping API calls
     * we should build something proper here.
     */

    private static final Logger LOG = LoggerFactory.getLogger(Ping.class);

    private final ObjectMapper objectMapper;
    private final AsyncHttpClient httpClient;
    private final String nodeId;
    private final URI serverUri;
    private final URI ourUri;

    @Inject
    public Ping(ObjectMapper objectMapper, AsyncHttpClient httpClient, @Named("rest_transport_uri") URI ourUri, @Named("graylog2_server_uri") URI serverUri, ServerStatus serverStatus) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.nodeId = serverStatus.getNodeId().toString();
        this.ourUri = ourUri;
        this.serverUri = serverUri;
    }

    public void ping() throws IOException, ExecutionException, InterruptedException {
        final PingRequest pingRequest = PingRequest.create(ourUri.toString());

        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUri);
        uriBuilder.path("/system/radios/" + nodeId + "/ping");

        final Request request = httpClient.preparePut(uriBuilder.build().toString())
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(pingRequest)).build();
        Future<Response> f = httpClient.executeRequest(request);

        Response r = f.get();

        // fail on a non-ok status
        if (r.getStatusCode() > 299) {
            throw new RuntimeException("Expected ping HTTP response OK but got [" + r.getStatusCode() + "]. Request was " + request.getUrl());
        }
    }

    @Override
    public void run() {
        try {
            ping();
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOG.error("Pinging the master node failed: ", e);
        }
    }
}
