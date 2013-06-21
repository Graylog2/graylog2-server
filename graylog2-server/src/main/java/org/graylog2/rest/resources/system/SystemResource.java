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

package org.graylog2.rest.resources.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.graylog2.rest.resources.RestResource;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system")
public class SystemResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemResource.class);

    @Context ResourceConfig rc;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String system(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("facility", "graylog2-server");
        result.put("codename", Core.GRAYLOG2_CODENAME);
        result.put("server_id", core.getServerId());
       	result.put("version", Core.GRAYLOG2_VERSION);
        result.put("started_at", core.getStartedAt().toString());

        return json(result, prettyPrint);
    }

    @GET
    @Path("/fields")
    @Produces(MediaType.APPLICATION_JSON)
    public String analyze(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("fields", core.getIndexer().getAllMessageFields());

        return json(result, prettyPrint);
    }

    @PUT
    @Path("/processing/pause")
    public Response pauseProcessing() {
        Core core = (Core) rc.getProperty("core");
        core.pauseMessageProcessing();

        LOG.info("Paused message processing - triggered by REST call.");
        return Response.ok().build();
    }

    @PUT
    @Path("/processing/resume")
    public Response resumeProcessing() {
        Core core = (Core) rc.getProperty("core");
        core.resumeMessageProcessing();

        LOG.info("Resumed message processing - triggered by REST call.");
        return Response.ok().build();
    }

}
