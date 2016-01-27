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
package org.graylog2.shared.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.jvm.ThreadDump;
import com.eaio.uuid.UUID;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.rest.models.system.responses.SystemJVMResponse;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.rest.models.system.responses.SystemThreadDumpResponse;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System", description = "System information of this node.")
@Path("/system")
@Produces(MediaType.APPLICATION_JSON)
public class SystemResource extends RestResource {
    private final ServerStatus serverStatus;
    private final ClusterId clusterId;

    @Inject
    public SystemResource(ServerStatus serverStatus, ClusterConfigService clusterConfigService) {
        this.serverStatus = serverStatus;
        this.clusterId = clusterConfigService.getOrDefault(ClusterId.class, ClusterId.create(UUID.nilUUID().toString()));
    }

    @GET
    @Timed
    @ApiOperation(value = "Get system overview")
    public SystemOverviewResponse system() {
        checkPermission(RestPermissions.SYSTEM_READ, serverStatus.getNodeId().toString());


        return SystemOverviewResponse.create("graylog-server",
                ServerVersion.CODENAME,
                serverStatus.getNodeId().toString(),
                clusterId.clusterId(),
                ServerVersion.VERSION.toString(),
                Tools.getISO8601String(serverStatus.getStartedAt()),
                serverStatus.isProcessing(),
                Tools.getLocalCanonicalHostname(),
                serverStatus.getLifecycle().getDescription().toLowerCase(Locale.ENGLISH),
                serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase(Locale.ENGLISH),
                serverStatus.getTimezone().getID(),
                System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "unknown"));
    }

    @GET
    @ApiOperation(value = "Get JVM information")
    @Path("/jvm")
    @Timed
    public SystemJVMResponse jvm() {
        checkPermission(RestPermissions.JVMSTATS_READ, serverStatus.getNodeId().toString());

        Runtime runtime = Runtime.getRuntime();
        return SystemJVMResponse.create(
                bytesToValueMap(runtime.freeMemory()),
                bytesToValueMap(runtime.maxMemory()),
                bytesToValueMap(runtime.totalMemory()),
                bytesToValueMap(runtime.totalMemory() - runtime.freeMemory()),
                serverStatus.getNodeId().toString(),
                Tools.getPID(),
                Tools.getSystemInformation());
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a thread dump")
    @Path("/threaddump")
    public SystemThreadDumpResponse threaddump() {
        checkPermission(RestPermissions.THREADS_DUMP, serverStatus.getNodeId().toString());

        // The ThreadDump is built by internal codahale.metrics servlet library we are abusing.
        final ThreadDump threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        threadDump.dump(output);
        return SystemThreadDumpResponse.create(new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    private Map<String, Long> bytesToValueMap(long bytes) {
        final Size size = Size.bytes(bytes);
        return ImmutableMap.of(
                "bytes", size.toBytes(),
                "kilobytes", size.toKilobytes(),
                "megabytes", size.toMegabytes());
    }
}
