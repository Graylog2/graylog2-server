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
import com.google.common.net.HttpHeaders;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.radio.responses.PersistedInputsResponse;
import org.graylog2.rest.models.radio.responses.PersistedInputsSummaryResponse;
import org.graylog2.rest.models.radio.responses.RegisterInputResponse;
import org.graylog2.rest.models.system.inputs.requests.RegisterInputRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class InputService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient httpclient;
    private final URI serverUrl;
    private final NodeId nodeId;

    @Inject
    public InputService(@Named("systemHttpClient") OkHttpClient httpclient,
                        @Named("graylog2_server_uri") URI serverUrl,
                        NodeId nodeId) {
        this.httpclient = checkNotNull(httpclient);
        this.serverUrl = checkNotNull(serverUrl);
        this.nodeId = checkNotNull(nodeId);
    }

    public List<PersistedInputsResponse> getPersistedInputs() throws IOException {
        final URI uri = UriBuilder.fromUri(serverUrl)
                .path("/system/radios/{radioId}/inputs")
                .build(nodeId.toString());

        final Request request = new Request.Builder()
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                .get()
                .url(uri.toString())
                .build();

        final Response r = httpclient.newCall(request).execute();
        if (!r.isSuccessful()) {
            throw new RuntimeException("Expected successful HTTP response [2xx] for list of persisted input but got [" + r.code() + "].");
        }

        final PersistedInputsSummaryResponse persistedInputsResponse = mapper.readValue(r.body().byteStream(), PersistedInputsSummaryResponse.class);

        return persistedInputsResponse.inputs();
    }

    public PersistedInputsResponse getPersistedInput(String inputId) throws IOException {
        for (PersistedInputsResponse inputSummaryResponse : getPersistedInputs()) {
            if (inputSummaryResponse.id().equals(inputId)) {
                return inputSummaryResponse;
            }
        }

        return null;
    }

    // TODO make this use a generic ApiClient class that knows the graylog-server node address(es) or something.
    public RegisterInputResponse registerInCluster(MessageInput input) throws ExecutionException, InterruptedException, IOException {
        final URI uri = UriBuilder.fromUri(serverUrl)
                .path("/system/radios/{radioId}/inputs")
                .build(nodeId.toString());

        final RegisterInputRequest rir = RegisterInputRequest.create(input.getId(), input.getTitle(), input.getType(), input.getConfiguration().getSource(),
                nodeId.toString(), input.getCreatorUserId());

        final Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), mapper.writeValueAsBytes(rir)))
                .url(uri.toString())
                .build();

        final Response r = httpclient.newCall(request).execute();
        final RegisterInputResponse registerInputResponse = mapper.readValue(r.body().byteStream(), RegisterInputResponse.class);

        // Set the ID that was generated in the server as persist ID of this input.
        input.setPersistId(registerInputResponse.persistId());

        if (!r.isSuccessful()) {
            throw new RuntimeException("Expected HTTP response [2xx] for input registration but got [" + r.code() + "].");
        }

        return registerInputResponse;
    }

    public void unregisterInCluster(MessageInput input) throws ExecutionException, InterruptedException, IOException {
        final URI uri = UriBuilder.fromUri(serverUrl)
                .path("/system/radios/{radioId}/inputs/{inputId}")
                .build(nodeId.toString(), input.getPersistId());

        final Request request = new Request.Builder()
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                .delete()
                .url(uri.toString())
                .build();

        final Response r = httpclient.newCall(request).execute();
        if (!r.isSuccessful()) {
            throw new RuntimeException("Expected HTTP response [2xx] for input unregistration but got [" + r.code() + "].");
        }
    }
}