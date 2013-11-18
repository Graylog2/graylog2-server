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

import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.jvm.ThreadDump;
import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.ProcessingPauseLockedException;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.util.Map;

import static javax.ws.rs.core.Response.ok;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "System", description = "System information of this node.")
@Path("/system")
public class SystemResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemResource.class);

    @GET @Timed
    @ApiOperation(value = "Get system overview")
    @Produces(MediaType.APPLICATION_JSON)
    public String system() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("facility", "graylog2-server");
        result.put("codename", Core.GRAYLOG2_CODENAME);
        result.put("server_id", core.getNodeId());
       	result.put("version", Core.GRAYLOG2_VERSION.toString());
        result.put("started_at", Tools.getISO8601String(core.getStartedAt()));
        result.put("is_processing", core.isProcessing());
        result.put("hostname", Tools.getLocalCanonicalHostname());

        return json(result);
    }

    @GET @Timed
    @ApiOperation(value = "Get list of all message fields that exist",
                  notes = "This operation is comparably fast because it reads directly from the indexer mapping.")
    @Path("/fields")
    @Produces(MediaType.APPLICATION_JSON)
    public String fields() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("fields", core.getIndexer().indices().getAllMessageFields());

        return json(result);
    }

    @PUT @Timed
    @ApiOperation(value = "Pauses message processing",
                  notes = "Inputs that are able to reject or requeue messages will do so, others will buffer messages in " +
                          "memory. Keep an eye on the heap space utilization while message processing is paused.")
    @Path("/processing/pause")
    public Response pauseProcessing() {
        core.pauseMessageProcessing(false);

        LOG.info("Paused message processing - triggered by REST call.");
        return ok().build();
    }

    @PUT @Timed
    @ApiOperation(value = "Resume message processing")
    @Path("/processing/resume")
    public Response resumeProcessing() {
        try {
            core.resumeMessageProcessing();
        } catch (ProcessingPauseLockedException e) {
            LOG.error("Message processing pause is locked. Returning HTTP 403.");
            throw new WebApplicationException(403);
        }

        LOG.info("Resumed message processing - triggered by REST call.");
        return ok().build();
    }

    @PUT @Timed
    @Path("/processing/pause/unlock")
    public Response unlockProcessingPause() {
        /*
         * This is meant to be only used in exceptional cases, when something that locked the processing pause
         * has crashed and never unlocked so we need to unlock manually. #donttellanybody
         */
        core.unlockProcessingPause();

        LOG.info("Manually unlocked message processing pause - triggered by REST call.");
        return ok().build();
    }

    @GET
    @ApiOperation(value = "Get JVM information")
    @Path("/jvm") @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String jvm() {
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> result = Maps.newHashMap();
        result.put("free_memory", bytesToValueMap(runtime.freeMemory()));
        result.put("max_memory",  bytesToValueMap(runtime.maxMemory()));
        result.put("total_memory", bytesToValueMap(runtime.totalMemory()));
        result.put("used_memory", bytesToValueMap(runtime.totalMemory() - runtime.freeMemory()));

        result.put("node_id", core.getNodeId());
        result.put("pid", Tools.getPID());
        result.put("info", Tools.getSystemInformation());

        return json(result);
    }

    @GET @Timed
    @ApiOperation(value = "Get a thread dump")
    @Path("/threaddump")
    @Produces(MediaType.TEXT_PLAIN)
    public String threaddump() {
        // The ThreadDump is built by internal codahale.metrics servlet library we are abusing.
        ThreadDump threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        threadDump.dump(output);
        return output.toString();
    }

    @GET @Timed
    @ApiOperation(value = "Get all available user permissions.")
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public String permissions() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("permissions", RestPermissions.allPermissions());
        return json(result);
    }

}
