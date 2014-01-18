/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.radio.inputs;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.inputs.api.InputSummaryResponse;
import org.graylog2.radio.inputs.api.PersistedInputsResponse;
import org.graylog2.radio.inputs.api.RegisterInputResponse;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.rest.resources.system.inputs.requests.RegisterInputRequest;
import org.joda.time.DateTime;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RadioInputRegistry extends InputRegistry {
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final AsyncHttpClient httpclient;
    protected final URI serverUrl;

    public RadioInputRegistry(InputHost core, AsyncHttpClient httpclient, URI serverUrl) {
        super(core);
        this.httpclient = httpclient;
        this.serverUrl = serverUrl;
    }

    protected MessageInput getMessageInput(InputSummaryResponse isr) {
        MessageInput input = null;
        try {
            input = InputRegistry.factory(isr.type);

            // Add all standard fields.
            input.initialize(new Configuration(isr.configuration), core);
            input.setTitle(isr.title);
            input.setCreatorUserId(isr.creatorUserId);
            input.setPersistId(isr.id);
            input.setCreatedAt(new DateTime(isr.createdAt));
            input.setGlobal(isr.global);

            input.checkConfiguration();
        } catch (NoSuchInputTypeException e) {
            LOG.warn("Cannot launch persisted input. No such type [{}].", isr.type);
            return null;
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input input configuration.", e);
            return null;
        }
        return input;
    }

    // TODO make this use a generic ApiClient class that knows the graylog2-server node address(es) or something.
    public void registerInCluster(MessageInput input) throws ExecutionException, InterruptedException, IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUrl);
        uriBuilder.path("/system/radios/" + core.getNodeId() + "/inputs");

        RegisterInputRequest rir = new RegisterInputRequest(input, core.getNodeId());

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
    }

    public void unregisterInCluster(MessageInput input) throws ExecutionException, InterruptedException, IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUrl);
        uriBuilder.path("/system/radios/" + core.getNodeId() + "/inputs/" + input.getPersistId());

        Future<Response> f = httpclient.prepareDelete(uriBuilder.build().toString()).execute();

        Response r = f.get();

        if (r.getStatusCode() != 204) {
            throw new RuntimeException("Expected HTTP response [204] for input unregistration but got [" + r.getStatusCode() + "].");
        }
    }

    // TODO make this use a generic ApiClient class that knows the graylog2-server node address(es) or something.
    public List<MessageInput> getAllPersisted() {
        final List<MessageInput> result = Lists.newArrayList();
        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUrl);
        uriBuilder.path("/system/radios/" + core.getNodeId() + "/inputs");

        List<InputSummaryResponse> response = null;
        try {
            Future<Response> f = httpclient.prepareGet(uriBuilder.build().toString()).execute();

            Response r = f.get();

            if (r.getStatusCode() != 200) {
                throw new RuntimeException("Expected HTTP response [200] for list of persisted input but got [" + r.getStatusCode() + "].");
            }
            response = mapper.readValue(r.getResponseBody(), PersistedInputsResponse.class).inputs;
        } catch (IOException e) {
            LOG.error("Unable to get persisted inputs: ", e);
            return result;
        } catch (InterruptedException e) {
            LOG.error("Unable to get persisted inputs: ", e);
            return result;
        } catch (ExecutionException e) {
            LOG.error("Unable to get persisted inputs: ", e);
            return result;
        }

        for (InputSummaryResponse isr : response) {
            result.add(getMessageInput(isr));
        }

        return result;
    }

    @Override
    protected void finishedLaunch(InputState state) {
        MessageInput input = state.getMessageInput();
        if (input.getPersistId() != null) {
            try {
                registerInCluster(input);
            } catch (Exception e) {
                LOG.error("Could not register input in Graylog2 cluster. It will be lost on next restart of this radio node.");
            }
        }
    }

    @Override
    public void cleanInput(MessageInput input) {
    }

    @Override
    protected void finishedTermination(InputState state) {
        MessageInput input = state.getMessageInput();
        try {
            unregisterInCluster(input);
        } catch (Exception e) {
            LOG.error("Could not unregister input [" + input.getName() + "] on server cluster.", e);
            return;
        }

        LOG.info("Unregistered input [" + input.getName() + "] on server cluster.");
    }
}
