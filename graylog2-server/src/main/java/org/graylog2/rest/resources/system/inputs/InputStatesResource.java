package org.graylog2.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.bundles.Input;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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

    @Inject
    public InputStatesResource(InputRegistry inputRegistry) {
        this.inputRegistry = inputRegistry;
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

    private InputStateSummary getInputStateSummary(IOState<MessageInput> inputState) {
        final MessageInput messageInput = inputState.getStoppable();
        return InputStateSummary.create(messageInput.getId(),
                inputState.getState().toString(),
                inputState.getStartedAt(),
                inputState.getDetailedMessage(),
                InputSummary.create(messageInput.getTitle(), messageInput.getPersistId(), messageInput.isGlobal(),
                        messageInput.getName(), messageInput.getContentPack(), messageInput.getId(),
                        messageInput.getCreatedAt(), messageInput.getType(), messageInput.getCreatorUserId(),
                        messageInput.getConfiguration().getSource(), messageInput.getStaticFields()));
    }
}
