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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.rest.models.system.buffers.responses.BufferClasses;
import org.graylog2.rest.models.system.buffers.responses.BuffersUtilizationSummary;
import org.graylog2.rest.models.system.buffers.responses.RingSummary;
import org.graylog2.rest.models.system.buffers.responses.SingleRingUtilization;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/Buffers", description = "Buffer information of this node.")
@Path("/system/buffers")
@Produces(MediaType.APPLICATION_JSON)
public class BuffersResource extends RestResource {

    private final Configuration configuration;
    private final InputBuffer inputBuffer;
    private final ProcessBuffer processBuffer;
    private final OutputBuffer outputBuffer;

    @Inject
    public BuffersResource(Configuration configuration,
                           ProcessBuffer processBuffer,
                           OutputBuffer outputBuffer) {
        this.configuration = configuration;
        this.inputBuffer = inputBuffer;
        this.processBuffer = processBuffer;
        this.outputBuffer = outputBuffer;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get current utilization of buffers and caches of this node.")
    @RequiresPermissions(RestPermissions.BUFFERS_READ)
    public BuffersUtilizationSummary utilization() {
        int ringSize = configuration.getRingSize();

        final long inputSize = processBuffer.size();
        final long inputUtil = inputSize/ringSize*100;

        final long processSize = processBuffer.size();
        final float processUtil = ((float) processSize / ringSize) * 100;
        process.put("utilization_percent", processUtil);
        process.put("utilization", processSize);

        final long outputSize = outputBuffer.size();
        final long outputUtil = outputSize/ringSize*100;

        return BuffersUtilizationSummary.create(
                RingSummary.create(
                        SingleRingUtilization.create(inputSize, inputUtil),
                        SingleRingUtilization.create(outputSize, outputUtil)
                )
        );
    }

    @GET
    @Timed
    @Path("/classes")
    @ApiOperation(value = "Get classnames of current buffer implementations.")
    @RequiresPermissions(RestPermissions.BUFFERS_READ)
    public BufferClasses getBufferClasses() {
        return BufferClasses.create(processBuffer.getClass().getCanonicalName(),
                outputBuffer.getClass().getCanonicalName());
    }
}
