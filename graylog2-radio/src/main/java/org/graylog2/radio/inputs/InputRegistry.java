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
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ning.http.client.Response;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.radio.Radio;
import org.graylog2.radio.inputs.api.InputSummaryResponse;
import org.graylog2.radio.inputs.api.PersistedInputsResponse;
import org.graylog2.radio.inputs.api.RegisterInputRequest;
import org.graylog2.radio.inputs.api.RegisterInputResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InputRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);

    private final Radio radio;
    private final List<InputState> inputStates;
    private final Map<String, String> availableInputs;

    private ObjectMapper mapper;

    private ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("inputs-%d").build()
    );

    public InputRegistry(InputHost radio) {
        this.radio = (Radio) radio;
        inputStates = Lists.newArrayList();
        availableInputs = Maps.newHashMap();

        mapper = new ObjectMapper();
    }

    public String launch(final MessageInput input, String id, boolean register) {
        final InputState inputState = new InputState(input, id);
        inputStates.add(inputState);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("Starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                try {
                    inputState.setState(InputState.InputStateType.STARTING);
                    input.launch();
                    inputState.setState(InputState.InputStateType.RUNNING);
                    LOG.info("Completed starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                } catch (MisfireException e) {
                    StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + "> " +
                            "was accepted but misfired. Reason: ").append(e.getMessage());

                    // Go down the whole cause chain to build a message that provides as much information as possible.
                    int maxLevel = 7; // ;)
                    Throwable cause = e.getCause();
                    for (int i = 0; i < maxLevel; i++) {
                        if (cause == null) {
                            break;
                        }

                        msg.append(", ").append(cause.getMessage());
                        cause = cause.getCause();
                    }

                    LOG.error(msg.toString(), e);

                    inputState.setState(InputState.InputStateType.FAILED);
                } catch(Exception e) {
                    LOG.error("Error in input <{}>", input.getId(), e);
                    inputState.setState(InputState.InputStateType.FAILED);
                }
            }
        });

        // Register in server cluster.
        if (register) {
            try {
                registerInCluster(input);
            } catch (Exception e) {
                LOG.error("Could not register input in Graylog2 cluster. It will be lost on next restart of this radio node.");
            }
        }

        return inputState.getId();
    }

    public void cleanInput(MessageInput input) {
        // Remove from running list.
        removeFromRunning(input);
    }

    public static MessageInput factory(String type) throws NoSuchInputTypeException {
        try {
            Class c = Class.forName(type);
            return (MessageInput) c.newInstance();
        } catch (ClassNotFoundException e) {
            throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
        } catch (Exception e) {
            throw new RuntimeException("Could not create input of type <" + type + ">", e);
        }
    }

    public void removeFromRunning(MessageInput input) {
        // Remove from running list.
        InputState thisInputState = null;
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().equals(input)) {
                thisInputState = inputState;
            }
        }
        inputStates.remove(thisInputState);
    }

    public String launchPersisted(InputSummaryResponse isr) {
        MessageInput input = null;
        try {
            input = InputRegistry.factory(isr.type);

            // Add all standard fields.
            input.initialize(new Configuration(isr.configuration), radio);
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

        return launch(input, isr.id, false);
    }

    public void launchAllPersisted() throws InterruptedException, ExecutionException, IOException {
        for (InputSummaryResponse isr : getAllPersisted()) {
            launchPersisted(isr);
        }
    }

    public String launch(final MessageInput input, Boolean register) {
        return launch(input, UUID.randomUUID().toString(), register);
    }

    // TODO make this use a generic ApiClient class that knows the graylog2-server node address(es) or something.
    public void registerInCluster(MessageInput input) throws ExecutionException, InterruptedException, IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(radio.getConfiguration().getGraylog2ServerUri());
        uriBuilder.path("/system/radios/" + radio.getNodeId() + "/inputs");

        RegisterInputRequest rir = new RegisterInputRequest(input, radio.getNodeId());

        String json;
        try {
            json = mapper.writeValueAsString(rir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create JSON for register input request.", e);
        }

        Future<Response> f = radio.getHttpClient().preparePost(uriBuilder.build().toString())
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
        final UriBuilder uriBuilder = UriBuilder.fromUri(radio.getConfiguration().getGraylog2ServerUri());
        uriBuilder.path("/system/radios/" + radio.getNodeId() + "/inputs/" + input.getPersistId());

        Future<Response> f = radio.getHttpClient().prepareDelete(uriBuilder.build().toString()).execute();

        Response r = f.get();

        if (r.getStatusCode() != 204) {
            throw new RuntimeException("Expected HTTP response [204] for input unregistration but got [" + r.getStatusCode() + "].");
        }
    }

    // TODO make this use a generic ApiClient class that knows the graylog2-server node address(es) or something.
    public List<InputSummaryResponse> getAllPersisted() throws ExecutionException, InterruptedException, IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(radio.getConfiguration().getGraylog2ServerUri());
        uriBuilder.path("/system/radios/" + radio.getNodeId() + "/inputs");

        Future<Response> f = radio.getHttpClient().prepareGet(uriBuilder.build().toString()).execute();

        Response r = f.get();

        if (r.getStatusCode() != 200) {
            throw new RuntimeException("Expected HTTP response [200] for list of persisted input but got [" + r.getStatusCode() + "].");
        }

        return mapper.readValue(r.getResponseBody(), PersistedInputsResponse.class).inputs;
    }

    public InputSummaryResponse getPersisted(String inputId) throws ExecutionException, InterruptedException, IOException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(radio.getConfiguration().getGraylog2ServerUri());
        uriBuilder.path("/system/radios/" + radio.getNodeId() + "/inputs");

        Future<Response> f = radio.getHttpClient().prepareGet(uriBuilder.build().toString()).execute();

        Response r = f.get();

        if (r.getStatusCode() != 200) {
            throw new RuntimeException("Expected HTTP response [200] for list of persisted input but got [" + r.getStatusCode() + "].");
        }

        List<InputSummaryResponse> response = mapper.readValue(r.getResponseBody(), PersistedInputsResponse.class).inputs;
        for (InputSummaryResponse isr : response) {
            if (isr.id.equals(inputId))
                return isr;
        }

        return null;
    }

    public void register(Class clazz, String name) {
        availableInputs.put(clazz.getCanonicalName(), name);
    }

    public List<InputState> getInputStates() {
        return inputStates;
    }

    public List<InputState> getRunningInputs() {
        List<InputState> runningInputs = Lists.newArrayList();
        for (InputState inputState : inputStates) {
            if (inputState.getState() == InputState.InputStateType.RUNNING)
                runningInputs.add(inputState);
        }
        return inputStates;
    }

    public boolean hasTypeRunning(Class klazz) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getClass().equals(klazz)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, String> getAvailableInputs() {
        return availableInputs;
    }

    public int runningCount() {
        return getRunningInputs().size();
    }

    public MessageInput getRunningInput(String inputId) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getId().equals(inputId))
                return inputState.getMessageInput();
        }

        return null;
    }
}
