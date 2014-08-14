/*
 * Copyright 2012-2014 TORCH GmbH
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

package org.graylog2.radio.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.inputs.api.InputSummaryResponse;
import org.graylog2.radio.inputs.api.PersistedInputsResponse;
import org.graylog2.radio.inputs.api.RegisterInputResponse;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.rest.resources.system.inputs.requests.RegisterInputRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(RadioInputRegistry.class);

    protected final ObjectMapper mapper = new ObjectMapper();
    protected final AsyncHttpClient httpclient;
    protected final URI serverUrl;
    private final ServerStatus serverStatus;

    public RadioInputRegistry(MessageInputFactory messageInputFactory,
                              ProcessBuffer processBuffer,
                              AsyncHttpClient httpclient,
                              URI serverUrl,
                              ServerStatus serverStatus) {
        super(messageInputFactory, processBuffer);
        this.httpclient = httpclient;
        this.serverUrl = serverUrl;
        this.serverStatus = serverStatus;
    }

    protected MessageInput getMessageInput(InputSummaryResponse isr) {
        MessageInput input;
        try {
            input = this.create(isr.type);

            Configuration inputConfig = new Configuration(isr.configuration);
            // Add all standard fields.
            input.setTitle(isr.title);
            input.setCreatorUserId(isr.creatorUserId);
            input.setPersistId(isr.id);
            input.setCreatedAt(new DateTime(isr.createdAt));
            input.setGlobal(isr.global);

            input.checkConfiguration(inputConfig);
            // initialize must run after all fields have been set. Why oh why isn't this done in the constructor/factory method?
            input.initialize(inputConfig);
        } catch (NoSuchInputTypeException e) {
            LOG.warn("Cannot launch persisted input. No such type [{}]. Error: {}", isr.type, e);
            return null;
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input input configuration.", e);
            return null;
        }
        return input;
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

    // TODO make this use a generic ApiClient class that knows the graylog2-server node address(es) or something.
    public List<MessageInput> getAllPersisted() {
        final List<MessageInput> result = Lists.newArrayList();
        final UriBuilder uriBuilder = UriBuilder.fromUri(serverUrl);
        uriBuilder.path("/system/radios/" + serverStatus.getNodeId().toString() + "/inputs");

        List<InputSummaryResponse> response;
        try {
            Request request = httpclient.prepareGet(uriBuilder.build().toString()).build();
            log.debug("API Request {} {}", request.getMethod(), request.getUrl());
            Future<Response> f = httpclient.executeRequest(request);

            Response r = f.get();

            if (r.getStatusCode() != 200) {
                throw new RuntimeException("Expected HTTP response [200] for list of persisted input but got [" + r.getStatusCode() + "].");
            }
            String responseBody = r.getResponseBody();
            PersistedInputsResponse persistedInputsResponse = mapper.readValue(responseBody,
                                                                               PersistedInputsResponse.class);
            response = persistedInputsResponse.inputs;
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
            final MessageInput messageInput = getMessageInput(isr);
            if (messageInput != null) {
                log.debug("Loaded message input {}", messageInput);
                result.add(messageInput);
            }
        }

        return result;
    }

    @Override
    protected void finishedLaunch(InputState state) {
    }

    @Override
    public void cleanInput(MessageInput input) {
    }

    @Override
    protected void finishedTermination(InputState state) {
        MessageInput input = state.getMessageInput();
        try {
            if (!state.getMessageInput().getGlobal())
                unregisterInCluster(input);
        } catch (Exception e) {
            LOG.error("Could not unregister input [{}], id <{}> on server cluster: {}", input.getName(), input.getId(), e);
            return;
        }

        LOG.info("Unregistered input [{}], id <{}> on server cluster.", input.getName(), input.getId());

        removeFromRunning(state);
    }

    @Override
    public InputState launch(MessageInput input, String id, boolean register) {
        if (register) {
            try {
                final RegisterInputResponse response = registerInCluster(input);
                if (response != null)
                    input.setPersistId(response.persistId);
            } catch (Exception e) {
                LOG.error("Could not register input in Graylog2 cluster. It will be lost on next restart of this radio node.", e);
                return null;
            }
        }
        return super.launch(input, id, register);
    }

    @Override
    protected void finishedStop(InputState inputState) {
    }
}
