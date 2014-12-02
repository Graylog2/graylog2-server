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
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.inputs.InputCache;
import org.graylog2.inputs.OutputCache;
import org.graylog2.plugin.buffers.BufferWatermark;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.shared.buffers.ProcessBuffer;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System/Buffers", description = "Buffer information of this node.")
@Path("/system/buffers")
public class BufferResource extends RestResource {

    private final InputCache inputCache;
    private final OutputCache outputCache;
    private final Configuration configuration;
    private final ProcessBuffer processBuffer;
    private final OutputBuffer outputBuffer;

    @Inject
    public BufferResource(InputCache inputCache,
                          OutputCache outputCache,
                          Configuration configuration,
                          ProcessBuffer processBuffer,
                          OutputBuffer outputBuffer) {
        this.inputCache = inputCache;
        this.outputCache = outputCache;
        this.configuration = configuration;
        this.processBuffer = processBuffer;
        this.outputBuffer = outputBuffer;
    }

    @GET @Timed
    @ApiOperation(value = "Get current utilization of buffers and caches of this node.")
    @RequiresPermissions(RestPermissions.BUFFERS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public String utilization() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("buffers", buffers());
        result.put("master_caches", masterCaches());

        return json(result);
    }

    @GET @Timed
    @Path("/classes")
    @ApiOperation(value = "Get classnames of current buffer implementations.")
    @RequiresPermissions(RestPermissions.BUFFERS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public String getBufferClasses() {
        Map<String, String> result = Maps.newHashMap();
        result.put("process_buffer", processBuffer.getClass().getCanonicalName());
        result.put("output_buffer", outputBuffer.getClass().getCanonicalName());

        return json(result);
    }

    private Map<String, Object> masterCaches() {
        Map<String, Object> caches = Maps.newHashMap();
        Map<String, Object> input = Maps.newHashMap();
        Map<String, Object> output = Maps.newHashMap();

        input.put("size", inputCache.size());
        output.put("size", outputCache.size());

        caches.put("input", input);
        caches.put("output", output);

        return caches;
    }

    private Map<String, Object> buffers() {
        Map<String, Object> buffers = Maps.newHashMap();
        Map<String, Object> input = Maps.newHashMap();
        Map<String, Object> output = Maps.newHashMap();

        int ringSize = configuration.getRingSize();

        final long inputSize = processBuffer.size();
        final float inputUtil = inputSize/ringSize*100;
        input.put("utilization_percent", inputUtil);
        input.put("utilization", inputSize);

        final long outputSize = outputBuffer.size();
        final float outputUtil = outputSize/ringSize*100;
        output.put("utilization_percent", outputUtil);
        output.put("utilization", outputSize);

        buffers.put("input", input);
        buffers.put("output", output);

        return buffers;
    }


}
