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
import com.google.common.eventbus.EventBus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/InputStates", description = "Message input states of this node")
@Path("/system/inputstates")
@Produces(MediaType.APPLICATION_JSON)
public class InputStatesResource extends RestResource {
    private final InputRegistry inputRegistry;
    private final EventBus serverEventBus;
    private final InputService inputService;

    @Inject
    public InputStatesResource(InputRegistry inputRegistry,
                               EventBus serverEventBus,
                               InputService inputService) {
        this.inputRegistry = inputRegistry;
        this.serverEventBus = serverEventBus;
        this.inputService = inputService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all input states of this node")
    public InputStatesList list() {
        final Set<InputStateSummary> result = this.inputRegistry.stream()
                .map(inputState -> getInputStateSummary(inputState))
                .collect(Collectors.toSet());

        return InputStatesList.create(result);
    }

    @GET
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Get input state for specified input id on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
    })
    public InputStateSummary get(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) {
        final IOState<MessageInput> inputState = this.inputRegistry.getInputState(inputId);
        if (inputState == null) {
            throw new NotFoundException("No input state for input id <" + inputId + "> on this node.");
        }
        return getInputStateSummary(inputState);
    }

    @PUT
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "(Re-)Start specified input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
    })
    public InputCreated start(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        inputService.find(inputId);
        final InputCreated result = InputCreated.create(inputId);
        this.serverEventBus.post(result);

        return result;
    }

    @DELETE
    @Path("/{inputId}")
    @Timed
    @ApiOperation(value = "Stop specified input on this node")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
    })
    public InputDeleted stop(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        inputService.find(inputId);
        final InputDeleted result = InputDeleted.create(inputId);
        this.serverEventBus.post(result);

        return result;
    }

    private InputStateSummary getInputStateSummary(IOState<MessageInput> inputState) {
        final MessageInput messageInput = inputState.getStoppable();
        return InputStateSummary.create(messageInput.getId(),
                inputState.getState().toString(),
                inputState.getStartedAt(),
                inputState.getDetailedMessage(),
                InputSummary.create(messageInput.getTitle(), messageInput.isGlobal(),
                        messageInput.getName(), messageInput.getContentPack(), messageInput.getId(),
                        messageInput.getCreatedAt(), messageInput.getType(), messageInput.getCreatorUserId(),
                        messageInput.getConfiguration().getSource(), messageInput.getStaticFields(), messageInput.getNodeId()));
    }
}
