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
package org.graylog2.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputsList;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.security.RestrictToMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/Inputs", description = "Message inputs")
@Path("/system/inputs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InputsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final Map<String, InputDescription> availableInputs;

    @Inject
    public InputsResource(InputService inputService, MessageInputFactory messageInputFactory) {
        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.availableInputs = messageInputFactory.getAvailableInputs();
    }

    @GET
    @Timed
    @ApiOperation(value = "Get information of a single input on this node")
    @Path("/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input.")
    })
    public InputSummary get(@ApiParam(name = "inputId", required = true)
                                      @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_READ, inputId);

        final Input input = inputService.find(inputId);

        return getInputSummary(input);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all inputs")
    public InputsList list() {
        final Set<InputSummary> inputs = inputService.all().stream()
                .map(input -> getInputSummary(input))
                .collect(Collectors.toSet());

        return InputsList.create(inputs);
    }

    @POST
    @Timed
    @ApiOperation(
            value = "Launch input on this node",
            response = InputCreated.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input type registered"),
            @ApiResponse(code = 400, message = "Missing or invalid configuration"),
            @ApiResponse(code = 400, message = "Type is exclusive and already has input running")
    })
    @RequiresPermissions(RestPermissions.INPUTS_CREATE)
    public Response create(@ApiParam(name = "JSON body", required = true)
                           @Valid @NotNull InputCreateRequest lr) throws ValidationException {
        try {
            // TODO Configuration type values need to be checked. See ConfigurationMapConverter.convertValues()
            final MessageInput messageInput = messageInputFactory.create(lr, getCurrentUser().getName(), lr.node());

            messageInput.checkConfiguration();
            final Input input = this.inputService.create(messageInput.asMap());
            final String newId = inputService.save(input);
            final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                    .path("{inputId}")
                    .build(newId);

            return Response.created(inputUri).entity(InputCreated.create(newId)).build();
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new NotFoundException("There is no such input type registered.", e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException("Missing or invalid input configuration.", e);
        }

    }

    @DELETE
    @Timed
    @Path("/{inputId}")
    @ApiOperation(value = "Terminate input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    public void terminate(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        final Input input = inputService.find(inputId);
        inputService.destroy(input);
    }

    @PUT
    @Timed
    @Path("/{inputId}")
    @ApiOperation(
            value = "Update input on this node",
            response = InputCreated.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
            @ApiResponse(code = 400, message = "Missing or invalid input configuration.")
    })
    public Response update(@ApiParam(name = "JSON body", required = true) @Valid @NotNull InputCreateRequest lr,
                           @ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException, NoSuchInputTypeException, ConfigurationException, ValidationException {
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        final Input input = inputService.find(inputId);

        final Map<String, Object> mergedInput = inputService.getMessageInput(input).asMap();
        final MessageInput messageInput = messageInputFactory.create(lr, getCurrentUser().getName(), lr.node());

        messageInput.checkConfiguration();

        mergedInput.putAll(messageInput.asMap());

        final Input newInput = inputService.create(input.getId(), mergedInput);
        inputService.save(newInput);

        final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                .path("{inputId}")
                .build(input.getId());

        return Response.created(inputUri).entity(InputCreated.create(input.getId())).build();
    }

    private InputSummary getInputSummary(Input input) {
        final InputDescription inputDescription = this.availableInputs.get(input.getType());
        final String name = inputDescription != null ? inputDescription.getName() : "Unknown Input (" + input.getType() + ")";
        return InputSummary.create(input.getTitle(),
                input.isGlobal(),
                name,
                input.getContentPack(),
                input.getId(),
                input.getCreatedAt(),
                input.getType(),
                input.getCreatorUserId(),
                input.getConfiguration(),
                input.getStaticFields(),
                input.getNodeId()
        );
    }
}
