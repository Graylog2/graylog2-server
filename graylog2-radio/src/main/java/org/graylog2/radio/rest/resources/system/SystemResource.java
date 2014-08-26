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
package org.graylog2.radio.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.jvm.ThreadDump;
import com.google.common.collect.Maps;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.radio.RadioVersion;
import org.graylog2.radio.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Path("/system")
public class SystemResource extends RestResource {
    @Inject
    private ServerStatus serverStatus;

    @GET @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String system() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("facility", "graylog2-radio");
        result.put("server_id", serverStatus.getNodeId().toString());
        result.put("version", RadioVersion.VERSION.toString());
        result.put("started_at", Tools.getISO8601String(serverStatus.getStartedAt()));
        result.put("hostname", Tools.getLocalCanonicalHostname());
        result.put("lifecycle", serverStatus.getLifecycle().getDescription().toLowerCase());
        result.put("lb_status", serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase());

        return json(result);
    }

    @GET @Timed
    @Path("/jvm")
    @Produces(MediaType.APPLICATION_JSON)
    public String jvm() {
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> result = Maps.newHashMap();
        result.put("free_memory", bytesToValueMap(runtime.freeMemory()));
        result.put("max_memory",  bytesToValueMap(runtime.maxMemory()));
        result.put("total_memory", bytesToValueMap(runtime.totalMemory()));
        result.put("used_memory", bytesToValueMap(runtime.totalMemory() - runtime.freeMemory()));

        result.put("node_id", serverStatus.getNodeId().toString());
        result.put("pid", Tools.getPID());
        result.put("info", Tools.getSystemInformation());

        return json(result);
    }

    @GET @Timed
    @Path("/threaddump")
    @Produces(MediaType.TEXT_PLAIN)
    public String threaddump() {
        // The ThreadDump is built by internal codahale.metrics servlet library we are abusing.
        ThreadDump threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        threadDump.dump(output);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

}
