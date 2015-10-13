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
package org.graylog2.rest.resources.system.radio;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.bson.types.ObjectId;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.radio.requests.PingRequest;
import org.graylog2.rest.models.radio.responses.PersistedInputsResponse;
import org.graylog2.rest.models.radio.responses.PersistedInputsSummaryResponse;
import org.graylog2.rest.models.radio.responses.RegisterInputResponse;
import org.graylog2.rest.models.system.inputs.requests.RegisterInputRequest;
import org.graylog2.rest.models.system.radio.responses.RadioSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Api(value = "System/Radios", description = "Management of graylog2-radio nodes.")
@Path("/system/radios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RadiosResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RadiosResource.class);

    private final NodeService nodeService;
    private final InputService inputService;

    @Inject
    public RadiosResource(NodeService nodeService, InputService inputService) {
        this.nodeService = nodeService;
        this.inputService = inputService;
    }

    @GET
    @Timed
    @ApiOperation(value = "List all active radios in this cluster.")
    public Map<String, Object> radios() {
        final List<RadioSummary> radioList = Lists.newArrayList();

        final Map<String, Node> radios = nodeService.allActive(Node.Type.RADIO);
        for (Map.Entry<String, Node> radio : radios.entrySet()) {
            radioList.add(radioSummary(radio.getValue()));
        }

        return ImmutableMap.of(
                "total", radios.size(),
                "radios", radioList);
    }

    @GET
    @Timed
    @ApiOperation(value = "Information about a radio.",
            notes = "This is returning information of a radio in context to its state in the cluster. " +
                    "Use the system API of the node itself to get system information.")
    @Path("/{radioId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found.")
    })
    public RadioSummary radio(@ApiParam(name = "radioId", required = true)
                              @PathParam("radioId") String radioId) {
        final Node radio;
        try {
            radio = nodeService.byNodeId(radioId);
        } catch (NodeNotFoundException e) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new NotFoundException(e);
        }

        if (radio == null) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new NotFoundException();
        }

        return radioSummary(radio);
    }


    @POST
    @Timed
    @ApiOperation(value = "Register input of a radio.",
            notes = "Radio inputs register their own inputs here for persistence after they successfully launched it.")
    @Path("/{radioId}/inputs")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found."),
            @ApiResponse(code = 400, message = "Missing or invalid configuration")
    })
    public Response registerInput(@ApiParam(name = "radioId", required = true)
                                  @PathParam("radioId") String radioId,
                                  @ApiParam(name = "JSON body", required = true)
                                  @Valid @NotNull RegisterInputRequest rir) throws ValidationException {
        final Node radio;
        try {
            radio = nodeService.byNodeId(radioId);
        } catch (NodeNotFoundException e) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new NotFoundException(e);
        }

        if (radio == null) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new NotFoundException();
        }

        // TODO: Make this cleaner.
        final Map<String, Object> inputData = Maps.newHashMap();
        if (rir.inputId() != null) {
            inputData.put("input_id", rir.inputId());
        } else {
            inputData.put("input_id", new ObjectId().toHexString());

            inputData.put("title", rir.title());
            inputData.put("type", rir.type());
            inputData.put("creator_user_id", rir.creatorUserId());
            inputData.put("configuration", rir.configuration());
            inputData.put("created_at", Tools.iso8601());
            inputData.put("radio_id", rir.radioId());
        }

        final Input mongoInput = inputService.create(inputData);

        // Write to database.
        final String id = inputService.save(mongoInput);

        final URI radioUri = getUriBuilderToSelf().path(RadiosResource.class)
                .path("{radioId}")
                .build(id);

        return Response.created(radioUri).entity(RegisterInputResponse.create(id)).build();
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Unregister input of a radio.",
            notes = "Radios unregister their inputs when they are stopped/terminated on the radio.")
    @Path("/{radioId}/inputs/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found.")
    })
    public void unregisterInput(@ApiParam(name = "radioId", required = true)
                                @PathParam("radioId") String radioId,
                                @ApiParam(name = "inputId", required = true)
                                @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        final Node radio;
        try {
            radio = nodeService.byNodeId(radioId);
        } catch (NodeNotFoundException e) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new NotFoundException("Radio <" + radioId + "> not found.");
        }

        if (radio == null) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new NotFoundException("Radio <" + radioId + "> not found.");
        }

        final Input input = inputService.findForThisRadioOrGlobal(radioId, inputId);
        if (!input.isGlobal()) {
            inputService.destroy(input);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Persisted inputs of a radio.",
            notes = "This is returning the configured persisted inputs of a radio node. This is *not* returning the actually " +
                    "running inputs on a radio node. Radio nodes use this resource to get their configured inputs on startup.")
    @Path("/{radioId}/inputs")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found.")
    })
    public PersistedInputsSummaryResponse persistedInputs(@ApiParam(name = "radioId", required = true)
                                               @PathParam("radioId") String radioId) {
        Node radio = null;
        try {
            radio = nodeService.byNodeId(radioId);
        } catch (NodeNotFoundException e) {
            LOG.debug("Radio <{}> not found.", radioId);
        }

        final List<PersistedInputsResponse> inputs = Lists.newArrayList();
        if (radio != null) {
            for (Input input : inputService.allOfRadio(radio))
                inputs.add(PersistedInputsResponse.create(input.getType(), input.getId(), input.getTitle(),
                        input.getCreatorUserId(), Tools.getISO8601String(input.getCreatedAt()), input.isGlobal(), input.getConfiguration()));
        }

        return PersistedInputsSummaryResponse.create(inputs);
    }


    @PUT
    @Timed
    @ApiOperation(value = "Ping - Accepts pings of graylog2-radio nodes.",
            notes = "Every graylog2-radio node is regularly pinging to announce that it is active.")
    @Path("/{radioId}/ping")
    public void ping(@ApiParam(name = "radioId", required = true)
                     @PathParam("radioId") String radioId,
                     @ApiParam(name = "JSON body", required = true)
                     @Valid @NotNull PingRequest pr) {
        LOG.debug("Ping from graylog2-radio node [{}].", radioId);

        Node node = null;
        try {
            node = nodeService.byNodeId(radioId);
        } catch (NodeNotFoundException e) {
            LOG.debug("There is no registered (or only outdated) graylog2-radio node [{}]. Registering.", radioId);
        }

        if (node != null) {
            nodeService.markAsAlive(node, false, pr.restTransportAddress());
            LOG.debug("Updated state of graylog2-radio node [{}].", radioId);
        } else {
            nodeService.registerRadio(radioId, pr.restTransportAddress());
        }
    }

    private RadioSummary radioSummary(Node node) {
        return RadioSummary.create(
                node.getNodeId(),
                node.getType().toString().toLowerCase(Locale.ENGLISH),
                node.getTransportAddress(),
                Tools.getISO8601String(node.getLastSeen()),
                node.getShortNodeId());
    }
}
