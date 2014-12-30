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
package org.graylog2.radio.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.cluster.InputService;
import org.graylog2.radio.rest.resources.RestResource;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.rest.models.system.inputs.responses.InputsList;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.inputs.PersistedInputs;
import org.graylog2.shared.rest.resources.system.inputs.requests.InputLaunchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Path("/system/inputs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    private final InputRegistry inputRegistry;
    private final MessageInputFactory messageInputFactory;
    private final InputLauncher inputLauncher;
    private final PersistedInputs persistedInputs;

    @Inject
    public InputsResource(InputRegistry inputRegistry,
                          MessageInputFactory messageInputFactory,
                          InputLauncher inputLauncher,
                          PersistedInputs persistedInputs) {
        this.inputRegistry = inputRegistry;
        this.messageInputFactory = messageInputFactory;
        this.inputLauncher = inputLauncher;
        this.persistedInputs = persistedInputs;
    }

    @GET
    @Timed
    public InputsList list() {
        final Set<InputStateSummary> inputStates = Sets.newHashSet();

        for (IOState<MessageInput> inputState : inputRegistry.getInputStates()) {
            final MessageInput input = inputState.getStoppable();
            inputStates.add(InputStateSummary.create(input.getId(),
                    inputState.getState().toString(),
                    inputState.getStartedAt(),
                    inputState.getDetailedMessage(),
                    InputSummary.create(input.getTitle(),
                            input.getId(),
                            input.isGlobal(),
                            input.getName(),
                            input.getContentPack(),
                            input.getId(),
                            input.getCreatedAt(),
                            input.getType(),
                            input.getCreatorUserId(),
                            input.getAttributesWithMaskedPasswords(),
                            input.getStaticFields()))
            );

        }

        return InputsList.create(inputStates);
    }

    @GET
    @Timed
    @Path("/{inputId}")
    public String single(@PathParam("inputId") String inputId) {
        final MessageInput input = inputRegistry.getRunningInput(inputId);

        if (input == null) {
            LOG.info("Input [{}] not found.", inputId);
            throw new NotFoundException();
        }

        return json(input.asMap());

    }

    @POST
    @Timed
    public Response launch(String body) {

        InputLaunchRequest lr;
        try {
            lr = objectMapper.readValue(body, InputLaunchRequest.class);
        } catch (IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new BadRequestException(e);
        }

        // Build a proper configuration from POST data.
        Configuration inputConfig = new Configuration(lr.configuration());

        // Build input.
        MessageInput input;
        try {
            input = messageInputFactory.create(lr.type(), inputConfig);
            input.setTitle(lr.title());
            input.setCreatorUserId(lr.creatorUserId());
            input.setCreatedAt(Tools.iso8601());
            input.setGlobal(lr.global());

            input.checkConfiguration();
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new BadRequestException(e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException(e);
        }

        String inputId = UUID.randomUUID().toString();
        input.setPersistId(inputId);

        // Don't run if exclusive and another instance is already running.
        if (input.isExclusive() && inputRegistry.hasTypeRunning(input.getClass())) {
            LOG.error("Type is exclusive and already has input running.");
            throw new BadRequestException();
        }

        input.initialize();

        // Launch input. (this will run async and clean up itself in case of an error.)
        inputLauncher.launch(input);

        final Map<String, String> result = ImmutableMap.of(
                "input_id", inputId,
                "persist_id", inputId);

        return Response.accepted().entity(json(result)).build();
    }


    @DELETE
    @Timed
    @Path("/{inputId}")
    public Response terminate(@PathParam("inputId") String inputId) {
        MessageInput input = inputRegistry.getRunningInput(inputId);

        if (input == null) {
            LOG.info("Cannot terminate input. Input not found.");
            throw new NotFoundException();
        }

        LOG.info("Attempting to terminate input [" + input.getName() + "]. Reason: REST request.");
        inputRegistry.remove(input);
        LOG.info("Terminated input [" + input.getName() + "]. Reason: REST request.");

        return Response.accepted().build();
    }

    @GET
    @Timed
    @Path("/types")
    public String types() {
        final Map<String, Object> result = Maps.newHashMap();
        final Map<String, InputDescription> availableInputs = messageInputFactory.getAvailableInputs();
        final Map<String, String> inputs = Maps.newHashMap();
        for (final String key : availableInputs.keySet()) {
            inputs.put(key, availableInputs.get(key).getName());
        }

        result.put("types", inputs);

        return json(result);
    }

    @GET
    @Timed
    @Path("/types/{inputType}")
    public String info(@PathParam("inputType") String inputType) {
        final Map<String, InputDescription> availableInputs = messageInputFactory.getAvailableInputs();
        if (!availableInputs.containsKey(inputType)) {
            LOG.error("Unknown input type {} requested.", inputType);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final InputDescription description = availableInputs.get(inputType);
        final Map<String, Object> result = Maps.newHashMap();
        result.put("type", inputType);
        result.put("name", description.getName());
        result.put("is_exclusive", description.isExclusive());
        result.put("requested_configuration", description.getRequestedConfiguration());
        result.put("link_to_docs", description.getLinkToDocs());

        return json(result);
    }

    @POST
    @Timed
    @Path("/{inputId}/launch")
    public Response launchExisting(@PathParam("inputId") String inputId) {
        final IOState<MessageInput> inputState = inputRegistry.getInputState(inputId);

        final MessageInput input;
        if (inputState == null) {
            input = persistedInputs.get(inputId);
        } else {
            input = inputState.getStoppable();
        }

        if (input == null) {
            final String message = "Cannot launch input <" + inputId + ">. Input not found.";
            LOG.info(message);
            throw new NotFoundException(message);
        }

        LOG.info("Launching existing input [" + input.getName() + "]. Reason: REST request.");
        input.initialize();
        inputLauncher.launch(input);
        LOG.info("Launched existing input [" + input.getName() + "]. Reason: REST request.");

        final Map<String, String> result = ImmutableMap.of(
                "input_id", inputId,
                "persist_id", inputId);

        return Response.accepted()
                .entity(json(result))
                .build();
    }

    @POST
    @Timed
    @Path("/{inputId}/stop")
    public Response stop(@PathParam("inputId") String inputId) {
        final MessageInput input = inputRegistry.getRunningInput(inputId);
        if (input == null) {
            LOG.info("Cannot stop input. Input not found.");
            throw new NotFoundException();
        }

        LOG.info("Stopping input [" + input.getName() + "]. Reason: REST request.");
        inputRegistry.stop(input);
        LOG.info("Stopped input [" + input.getName() + "]. Reason: REST request.");

        return Response.accepted().build();
    }

    @POST
    @Timed
    @Path("/{inputId}/restart")
    public Response restart(@PathParam("inputId") String inputId) {
        stop(inputId);
        launchExisting(inputId);
        return Response.accepted().build();
    }
}
