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

import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.graylog2.Core;
import org.graylog2.ProcessingPauseLockedException;
import org.graylog2.plugin.Tools;
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
        core.pauseMessageProcessing(false);

        LOG.info("Paused message processing - triggered by REST call.");
        return Response.ok().build();
    }

    @PUT
    @Path("/processing/resume")
    public Response resumeProcessing() {
        Core core = (Core) rc.getProperty("core");

        try {
            core.resumeMessageProcessing();
        } catch (ProcessingPauseLockedException e) {
            LOG.error("Message processing pause is locked. Returning HTTP 403.");
            throw new WebApplicationException(403);
        }

        LOG.info("Resumed message processing - triggered by REST call.");
        return Response.ok().build();
    }

    @PUT
    @Path("/processing/pause/unlock")
    public Response unlockProcessingPause() {

        /*
         * This is meant to be only used in exceptional cases, when something that locked the processing pause
         * has crashed and never unlocked so we need to unlock manually. #donttellanybody
         */

        Core core = (Core) rc.getProperty("core");
        core.unlockProcessingPause();

        LOG.info("Manually unlocked message processing pause - triggered by REST call.");
        return Response.ok().build();
    }

    @GET
    @Path("/jvm")
    @Produces(MediaType.APPLICATION_JSON)
    public String jvm(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> result = Maps.newHashMap();
        result.put("free_memory", bytesToValueMap(runtime.freeMemory()));
        result.put("max_memory",  bytesToValueMap(runtime.maxMemory()));
        result.put("total_memory", bytesToValueMap(runtime.totalMemory()));
        result.put("used_memory", bytesToValueMap(runtime.totalMemory() - runtime.freeMemory()));

        result.put("node_id", core.getServerId());
        result.put("pid", Tools.getPID());
        result.put("info", Tools.getSystemInformation());

        return json(result, prettyPrint);
    }

}
