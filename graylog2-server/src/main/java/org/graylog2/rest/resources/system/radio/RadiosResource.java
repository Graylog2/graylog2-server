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
package org.graylog2.rest.resources.system.radio;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.cluster.Node;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.radio.requests.PingRequest;
import org.graylog2.rest.resources.system.radio.requests.RegisterInputRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.ok;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
// @RequiresAuthentication unauthenticated because radios do not have any authentication support yet
@Api(value = "System/Radios", description = "Management of graylog2-radio nodes.")
@Path("/system/radios")
public class RadiosResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RadiosResource.class);

    @GET @Timed
    @ApiOperation(value = "List all active radios in this cluster.")
    public String radios() {
        List<Map<String, Object>> radioList = Lists.newArrayList();
        Map<String, Node> radios = Node.allActive(core, Node.Type.RADIO);

        for(Map.Entry<String, Node> radio : radios.entrySet()) {
            radioList.add(radioSummary(radio.getValue()));
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("total", radios.size());
        result.put("radios", radioList);

        return json(result);
    }

    @GET @Timed
    @ApiOperation(value = "Information about a radio.",
            notes = "This is returning information of a radio in context to its state in the cluster. " +
                    "Use the system API of the node itself to get system information.")
    @Path("/{radioId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found.")
    })
    public String radio(@ApiParam(title = "radioId", required = true) @PathParam("radioId") String radioId) {
        Node radio = Node.byNodeId(core, radioId);

        if (radio == null) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new WebApplicationException(404);
        }

        return json(radioSummary(radio));
    }


    @POST @Timed
    @ApiOperation(value = "Register input of a radio.",
            notes = "Radio inputs register their own inputs here for persistence after they successfully launched it.")
    @Path("/{radioId}/inputs")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found."),
            @ApiResponse(code = 400, message = "Missing or invalid configuration")
    })
    public Response registerInput(@ApiParam(title = "JSON body", required = true) String body,
                                @ApiParam(title = "radioId", required = true) @PathParam("radioId") String radioId) {
        Node radio = Node.byNodeId(core, radioId);

        if (radio == null) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new WebApplicationException(404);
        }

        RegisterInputRequest rir;
        try {
            rir = objectMapper.readValue(body, RegisterInputRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> inputData = Maps.newHashMap();
        inputData.put("input_id", rir.inputId);
        inputData.put("title", rir.title);
        inputData.put("type", rir.type);
        inputData.put("creator_user_id", rir.creatorUserId);
        inputData.put("configuration", rir.configuration);
        inputData.put("created_at", new DateTime(DateTimeZone.UTC));
        inputData.put("radio_id", rir.radioId);

        Input mongoInput = new Input(core, inputData);

        // Write to database.
        ObjectId id;
        try {
            id = mongoInput.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("persist_id", id.toStringMongod());

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @DELETE @Timed
    @ApiOperation(value = "Unregister input of a radio.",
            notes = "Radios unregister their inputs when they are stopped/terminated on the radio.")
    @Path("/{radioId}/inputs/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found.")
    })
    public Response unregisterInput(@ApiParam(title = "radioId", required = true) @PathParam("radioId") String radioId,
                                    @ApiParam(title = "inputId", required = true) @PathParam("inputId") String inputId) {
        Node radio = Node.byNodeId(core, radioId);

        if (radio == null) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new WebApplicationException(404);
        }

        DBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(inputId));
        query.put("radio_id", radioId);

        Input.destroy(query, core, Input.COLLECTION);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET @Timed
    @ApiOperation(value = "Persisted inputs of a radio.",
            notes = "This is returning the configured persisted inputs of a radio node. This is *not* returning the actually " +
                    "running inputs on a radio node. Radio nodes use this resource to get their configured inputs on startup.")
    @Path("/{radioId}/inputs")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Radio not found.")
    })
    public String persistedInputs(@ApiParam(title = "radioId", required = true) @PathParam("radioId") String radioId) {
        Node radio = Node.byNodeId(core, radioId);

        if (radio == null) {
            LOG.error("Radio <{}> not found.", radioId);
            throw new WebApplicationException(404);
        }

        Map<String, Object> result = Maps.newHashMap();
        List<Map<String, Object>> inputs = Lists.newArrayList();

        for (Input input : Input.allOfRadio(core, radio)) {
            Map<String, Object> inputSummary = Maps.newHashMap();

            inputSummary.put("type", input.getType());
            inputSummary.put("id", input.getId());
            inputSummary.put("title", input.getTitle());
            inputSummary.put("configuration", input.getConfiguration());
            inputSummary.put("creator_user_id", input.getCreatorUserId());
            inputSummary.put("created_at", Tools.getISO8601String(input.getCreatedAt()));
            inputSummary.put("global", input.isGlobal());

            inputs.add(inputSummary);
        }

        result.put("inputs", inputs);
        result.put("total", inputs.size());

        return json(result);
    }


    @PUT @Timed
    @ApiOperation(value = "Ping - Accepts pings of graylog2-radio nodes.",
            notes = "Every graylog2-radio node is regularly pinging to announce that it is active.")
    @Path("/{radioId}/ping")
    public Response ping(@ApiParam(title = "JSON body", required = true) String body,
                         @ApiParam(title = "radioId", required = true) @PathParam("radioId") String radioId) {
        PingRequest pr;

        try {
            pr = objectMapper.readValue(body, PingRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        LOG.debug("Ping from graylog2-radio node [{}].", radioId);

        Node node = Node.byNodeId(core, radioId);

        if (node != null) {
            node.markAsAlive(false, pr.restTransportAddress);
            LOG.debug("Updated state of graylog2-radio node [{}].", radioId);
        } else {
            LOG.debug("There is no registered (or only outdated) graylog2-radio node [{}]. Registering.", radioId);
            Node.registerRadio(core, radioId, pr.restTransportAddress);
        }

        return ok().build();
    }

    private Map<String, Object> radioSummary(Node node) {
        Map<String, Object> m  = Maps.newHashMap();

        m.put("id", node.getNodeId());
        m.put("type", node.getType().toString().toLowerCase());
        m.put("transport_address", node.getTransportAddress());
        m.put("last_seen", Tools.getISO8601String(node.getLastSeen()));

        // Only meant to be used for representation. Not a real ID.
        m.put("short_node_id", node.getShortNodeId());

        return m;
    }

}
