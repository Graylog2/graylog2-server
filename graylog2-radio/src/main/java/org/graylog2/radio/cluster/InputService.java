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
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.inputs.api.InputSummaryResponse;
import org.graylog2.radio.inputs.api.PersistedInputsResponse;
import org.graylog2.radio.inputs.api.RegisterInputResponse;
import org.graylog2.shared.rest.resources.system.inputs.requests.RegisterInputRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class InputService {
    private static final Logger LOG = LoggerFactory.getLogger(InputService.class);

    protected final ObjectMapper mapper = new ObjectMapper();
    protected final AsyncHttpClient httpclient;
    protected final URI serverUrl;
    private final ServerStatus serverStatus;

    @Inject
    public InputService(AsyncHttpClient httpclient, @Named("ServerUri") URI serverUrl, ServerStatus serverStatus) {
        this.httpclient = httpclient;
        this.serverUrl = serverUrl;
        this.serverStatus = serverStatus;
    }

    public List<InputSummaryResponse> getPersistedInputs() throws IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUrl);
        uriBuilder.path("/system/radios/" + serverStatus.getNodeId().toString() + "/inputs");

        final Request request = httpclient.prepareGet(uriBuilder.build().toString()).build();
        LOG.debug("API Request {} {}", request.getMethod(), request.getUrl());
        final Future<Response> f = httpclient.executeRequest(request);

        final Response r;
        try {
            r = f.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to fetch inputs from master: ", e);
            return Collections.emptyList();
        }

        if (r.getStatusCode() != 200) {
            throw new RuntimeException("Expected HTTP response [200] for list of persisted input but got [" + r.getStatusCode() + "].");
        }
        final String responseBody = r.getResponseBody();
        PersistedInputsResponse persistedInputsResponse = mapper.readValue(responseBody,
                PersistedInputsResponse.class);
        return persistedInputsResponse.inputs;
    }

    public InputSummaryResponse getPersistedInput(String inputId) throws IOException {
        for (InputSummaryResponse inputSummaryResponse : getPersistedInputs())
            if (inputSummaryResponse.id.equals(inputId))
                return inputSummaryResponse;

        return null;
    }

    // TODO make this use a generic ApiClient class that knows the graylog2-server node address(es) or something.
    public RegisterInputResponse registerInCluster(MessageInput input) throws ExecutionException, InterruptedException, IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUrl);
        uriBuilder.path("/system/radios/" + serverStatus.getNodeId().toString() + "/inputs");

        RegisterInputRequest rir = new RegisterInputRequest(input, serverStatus.getNodeId().toString());

        String json;
        try {
            json = mapper.writeValueAsString(rir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create JSON for register input request.", e);
        }

        Future<Response> f = httpclient.preparePost(uriBuilder.build().toString())
                .setBody(json)
                .execute();

        Response r = f.get();

        RegisterInputResponse response = mapper.readValue(r.getResponseBody(), RegisterInputResponse.class);

        // Set the ID that was generated in the server as persist ID of this input.
        input.setPersistId(response.persistId);

        if (r.getStatusCode() != 201) {
            throw new RuntimeException("Expected HTTP response [201] for input registration but got [" + r.getStatusCode() + "].");
        }

        return response;
    }

    public void unregisterInCluster(MessageInput input) throws ExecutionException, InterruptedException, IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUrl);
        uriBuilder.path("/system/radios/" + serverStatus.getNodeId().toString() + "/inputs/" + input.getPersistId());

        Future<Response> f = httpclient.prepareDelete(uriBuilder.build().toString()).execute();
        Response r = f.get();
        if (r.getStatusCode() != 204) {
            throw new RuntimeException("Expected HTTP response [204] for input unregistration but got [" + r.getStatusCode() + "].");
        }
    }
}