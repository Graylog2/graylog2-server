/**
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
 */
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.jvm.ThreadDump;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.ServerVersion;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.ProcessingPauseLockedException;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.responses.ReaderPermissionResponse;
import org.graylog2.security.RestPermissions;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.accepted;

@RequiresAuthentication
@Api(value = "System", description = "System information of this node.")
@Path("/system")
public class SystemResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemResource.class);

    private final ServerStatus serverStatus;
    private final GracefulShutdown gracefulShutdown;
    private final Indices indices;

    @Inject
    public SystemResource(ServerStatus serverStatus,
                          Indices indices,
                          GracefulShutdown gracefulShutdown) {
        this.serverStatus = serverStatus;
        this.indices = indices;
        this.gracefulShutdown = gracefulShutdown;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get system overview")
    @Produces(APPLICATION_JSON)
    public Map<String, Object> system() {
        checkPermission(RestPermissions.SYSTEM_READ, serverStatus.getNodeId().toString());
        Map<String, Object> result = Maps.newHashMap();
        result.put("facility", "graylog2-server");
        result.put("codename", ServerVersion.CODENAME);
        result.put("server_id", serverStatus.getNodeId().toString());
        result.put("version", ServerVersion.VERSION.toString());
        result.put("started_at", Tools.getISO8601String(serverStatus.getStartedAt()));
        result.put("is_processing", serverStatus.isProcessing());
        result.put("hostname", Tools.getLocalCanonicalHostname());
        result.put("lifecycle", serverStatus.getLifecycle().getDescription().toLowerCase());
        result.put("lb_status", serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase());
        result.put("timezone", serverStatus.getTimezone().getID());

        return result;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get list of message fields that exist",
            notes = "This operation is comparably fast because it reads directly from the indexer mapping.")
    @Path("/fields")
    @RequiresPermissions(RestPermissions.FIELDNAMES_READ)
    @Produces(APPLICATION_JSON)
    public Map<String, Set<String>> fields(@ApiParam(name = "limit", value = "Maximum number of fields to return. Set to 0 for all fields.", required = false)
                                           @QueryParam("limit") int limit) {
        boolean unlimited = limit <= 0;

        final Set<String> fields;
        if (unlimited) {
            fields = indices.getAllMessageFields();
        } else {
            fields = Sets.newHashSet();
            int i = 0;
            for (String field : indices.getAllMessageFields()) {
                if (i == limit) {
                    break;
                }

                fields.add(field);
                i++;
            }
        }

        return ImmutableMap.of("fields", fields);
    }

    // TODO Change to @POST
    @PUT
    @Timed
    @ApiOperation(value = "Pauses message processing",
            notes = "Inputs that are able to reject or requeue messages will do so, others will buffer messages in " +
                    "memory. Keep an eye on the heap space utilization while message processing is paused.")
    @Path("/processing/pause")
    public void pauseProcessing() {
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, serverStatus.getNodeId().toString());
        serverStatus.pauseMessageProcessing(false);

        LOG.info("Paused message processing - triggered by REST call.");
    }

    @PUT
    @Timed
    @ApiOperation(value = "Resume message processing")
    @Path("/processing/resume")
    public void resumeProcessing() {
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, serverStatus.getNodeId().toString());

        try {
            serverStatus.resumeMessageProcessing();
        } catch (ProcessingPauseLockedException e) {
            LOG.error("Message processing pause is locked. Returning HTTP 403.");
            throw new ForbiddenException(e);
        }

        LOG.info("Resumed message processing - triggered by REST call.");
    }

    @PUT
    @Timed
    @Path("/processing/pause/unlock")
    public void unlockProcessingPause() {
        /*
         * This is meant to be only used in exceptional cases, when something that locked the processing pause
         * has crashed and never unlocked so we need to unlock manually. #donttellanybody
         */
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, serverStatus.getNodeId().toString());

        serverStatus.unlockProcessingPause();

        LOG.info("Manually unlocked message processing pause - triggered by REST call.");
    }

    @GET
    @ApiOperation(value = "Get JVM information")
    @Path("/jvm")
    @Timed
    @Produces(APPLICATION_JSON)
    public Map<String, Object> jvm() {
        checkPermission(RestPermissions.JVMSTATS_READ, serverStatus.getNodeId().toString());

        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> result = Maps.newHashMap();
        result.put("free_memory", bytesToValueMap(runtime.freeMemory()));
        result.put("max_memory", bytesToValueMap(runtime.maxMemory()));
        result.put("total_memory", bytesToValueMap(runtime.totalMemory()));
        result.put("used_memory", bytesToValueMap(runtime.totalMemory() - runtime.freeMemory()));

        result.put("node_id", serverStatus.getNodeId().toString());
        result.put("pid", Tools.getPID());
        result.put("info", Tools.getSystemInformation());

        return result;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a thread dump")
    @Path("/threaddump")
    @Produces(TEXT_PLAIN)
    public String threaddump() {
        checkPermission(RestPermissions.THREADS_DUMP, serverStatus.getNodeId().toString());

        // The ThreadDump is built by internal codahale.metrics servlet library we are abusing.
        final ThreadDump threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        threadDump.dump(output);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @GET
    @Timed
    @RequiresGuest // turns off authentication for this action
    @ApiOperation(value = "Get all available user permissions.")
    @Path("/permissions")
    @Produces(APPLICATION_JSON)
    public Map<String, Map<String, Collection<String>>> permissions() {
        return ImmutableMap.of("permissions", RestPermissions.allPermissions());
    }

    @GET
    @Timed
    @RequiresGuest
    @ApiOperation(value = "Get the initial permissions assigned to a reader account")
    @Path("/permissions/reader/{username}")
    @Produces(APPLICATION_JSON)
    public ReaderPermissionResponse readerPermissions(
            @ApiParam(name = "username", required = true)
            @PathParam("username") String username) {
        return ReaderPermissionResponse.create(
                Ordering.natural().sortedCopy(RestPermissions.readerPermissions(username)));
    }

    @POST
    @Timed
    @ApiOperation(value = "Shutdown this node gracefully.",
            notes = "Attempts to process all buffered and cached messages before exiting, " +
                    "shuts down inputs first to make sure that no new messages are accepted.")
    @Path("/shutdown")
    public Response shutdown() {
        checkPermission(RestPermissions.NODE_SHUTDOWN, serverStatus.getNodeId().toString());

        new Thread(gracefulShutdown).start();
        return accepted().build();
    }
}
