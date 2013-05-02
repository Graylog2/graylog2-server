/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.rest.resources.streams;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.RestResource;
import org.graylog2.rest.resources.streams.requests.CreateRequest;
import org.graylog2.streams.StreamImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/streams")
public class StreamResource extends RestResource {
	private static final Logger LOG = LoggerFactory.getLogger(StreamResource.class);

    @Context ResourceConfig rc;

    @POST @Path("/")
    @Consumes(MediaType.APPLICATION_JSON) 
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String body, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (body == null || body.isEmpty()) {
        	LOG.error("Missing parameters. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

        CreateRequest cr;
        try {
            cr = objectMapper.readValue(body, CreateRequest.class);
        } catch(IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Status.BAD_REQUEST);
        }

        // Create stream.
        Map<String, Object> streamData = Maps.newHashMap();
        streamData.put("title", cr.title);
        streamData.put("creator_user_id", cr.creator_user_id);
        streamData.put("created_at", new DateTime(DateTimeZone.UTC));

        StreamImpl stream = new StreamImpl(streamData, core);
        ObjectId id;
        try {
            id = stream.save();
        } catch (ValidationException e) {
            LOG.error("Validation error.", e);
            throw new WebApplicationException(e, Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("stream_id", id.toStringMongod());

        return Response.status(Status.CREATED).entity(json(result, prettyPrint)).build();
    }
    
    @GET @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");
        
        List<Map<String, Object>> streams = Lists.newArrayList();
        for (Stream stream : StreamImpl.loadAllEnabled(core)) {
        	streams.add(((StreamImpl) stream).asMap());
        }
        
        Map<String, Object> result = Maps.newHashMap();
        result.put("total", streams.size());
        result.put("streams", streams);

        return json(result, prettyPrint);
    }
    
    @GET @Path("/{streamId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("streamId") String streamId, @QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        if (streamId == null || streamId.isEmpty()) {
        	LOG.error("Missing streamId. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

        StreamImpl stream;
        try {
        	stream = StreamImpl.load(loadObjectId(streamId), core);
        } catch (NotFoundException e) {
        	throw new WebApplicationException(404);
        }

        return json(stream.asMap(), prettyPrint);
    }

    @DELETE @Path("/{streamId}")
    public Response delete(@PathParam("streamId") String streamId) {
        Core core = (Core) rc.getProperty("core");

        if (streamId == null || streamId.isEmpty()) {
        	LOG.error("Missing streamId. Returning HTTP 400.");
        	throw new WebApplicationException(400);
        }

        try {
        	StreamImpl stream = StreamImpl.load(loadObjectId(streamId), core);
        	stream.destroy();
        } catch (NotFoundException e) {
        	throw new WebApplicationException(404);
        }
        
        return Response.status(Status.fromStatusCode(204)).build();
    }
}
