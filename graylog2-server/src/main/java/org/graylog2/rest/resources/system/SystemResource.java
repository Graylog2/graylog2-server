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
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Core;
import org.graylog2.ProcessingPauseLockedException;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.responses.ReaderPermissionResponse;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.accepted;
import static javax.ws.rs.core.Response.ok;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System", description = "System information of this node.")
@Path("/system")
public class SystemResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemResource.class);

    @GET @Timed
    @ApiOperation(value = "Get system overview")
    @Produces(APPLICATION_JSON)
    public String system() {
        checkPermission(RestPermissions.SYSTEM_READ, core.getNodeId());
        Map<String, Object> result = Maps.newHashMap();
        result.put("facility", "graylog2-server");
        result.put("codename", Core.GRAYLOG2_CODENAME);
        result.put("server_id", core.getNodeId());
       	result.put("version", Core.GRAYLOG2_VERSION.toString());
        result.put("started_at", Tools.getISO8601String(core.getStartedAt()));
        result.put("is_processing", core.isProcessing());
        result.put("hostname", Tools.getLocalCanonicalHostname());
        result.put("lifecycle", core.getLifecycle().getName().toLowerCase());
        result.put("lb_status", core.getLifecycle().getLoadbalancerStatus().toString().toLowerCase());

        return json(result);
    }

    @GET @Timed
    @ApiOperation(value = "Get list of message fields that exist",
                  notes = "This operation is comparably fast because it reads directly from the indexer mapping.")
    @Path("/fields")
    @RequiresPermissions(RestPermissions.FIELDNAMES_READ)
    @Produces(APPLICATION_JSON)
    public String fields(@ApiParam(title = "limit", description = "Maximum number of fields to return. Set to 0 for all fields.", required = false) @QueryParam("limit") int limit) {
        boolean unlimited = limit <= 0;

        Set<String> fields;
        if (unlimited) {
            fields = core.getIndexer().indices().getAllMessageFields();
        } else {
            fields = Sets.newHashSet();
            int i = 0;
            for (String field : core.getIndexer().indices().getAllMessageFields()) {
                if (i == limit) {
                    break;
                }

                fields.add(field);
                i++;
            }

        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("fields", fields);

        return json(result);
    }

    @PUT @Timed
    @ApiOperation(value = "Pauses message processing",
                  notes = "Inputs that are able to reject or requeue messages will do so, others will buffer messages in " +
                          "memory. Keep an eye on the heap space utilization while message processing is paused.")
    @Path("/processing/pause")
    public Response pauseProcessing() {
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, core.getNodeId());
        core.pauseMessageProcessing(false);

        LOG.info("Paused message processing - triggered by REST call.");
        return ok().build();
    }

    @PUT @Timed
    @ApiOperation(value = "Resume message processing")
    @Path("/processing/resume")
    public Response resumeProcessing() {
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, core.getNodeId());

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
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, core.getNodeId());

        core.unlockProcessingPause();

        LOG.info("Manually unlocked message processing pause - triggered by REST call.");
        return ok().build();
    }

    @GET
    @ApiOperation(value = "Get JVM information")
    @Path("/jvm") @Timed
    @Produces(APPLICATION_JSON)
    public String jvm() {
        checkPermission(RestPermissions.JVMSTATS_READ, core.getNodeId());

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
    @Produces(TEXT_PLAIN)
    public String threaddump() {
        checkPermission(RestPermissions.THREADS_DUMP, core.getNodeId());

        // The ThreadDump is built by internal codahale.metrics servlet library we are abusing.
        ThreadDump threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        threadDump.dump(output);
        return output.toString();
    }

    @GET @Timed
    @RequiresGuest // turns off authentication for this action
    @ApiOperation(value = "Get all available user permissions.")
    @Path("/permissions")
    @Produces(APPLICATION_JSON)
    public String permissions() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("permissions", RestPermissions.allPermissions());
        return json(result);
    }

    @GET @Timed
    @RequiresGuest
    @ApiOperation(value = "Get the initial permissions assigned to a reader account")
    @Path("/permissions/reader/{username}")
    @Produces(APPLICATION_JSON)
    public ReaderPermissionResponse readerPermissions(
            @ApiParam(title = "username", required = true)
            @PathParam("username") String username) {
        if (username == null || username.isEmpty()) {
            throw new BadRequestException("Username cannot be null or empty");
        }
        final ReaderPermissionResponse response = new ReaderPermissionResponse();
        response.permissions = Ordering.natural().sortedCopy(RestPermissions.readerPermissions(username));
        return response;
    }

    @POST @Timed
    @ApiOperation(value = "Shutdown this node gracefully.",
                  notes = "Attempts to process all buffered and cached messages before exiting, " +
                          "shuts down inputs first to make sure that no new messages are accepted.")
    @Path("/shutdown")
    public Response shutdown() {
        checkPermission(RestPermissions.NODE_SHUTDOWN, core.getNodeId());

        new Thread(new GracefulShutdown(core)).start();
        return accepted().build();
    }

}
