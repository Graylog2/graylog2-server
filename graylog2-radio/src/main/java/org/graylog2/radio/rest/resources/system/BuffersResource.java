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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.graylog2.radio.Configuration;
import org.graylog2.rest.models.system.buffers.responses.BuffersUtilizationSummary;
import org.graylog2.rest.models.system.buffers.responses.RingSummary;
import org.graylog2.rest.models.system.buffers.responses.SingleRingUtilization;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "System/Buffers", description = "Buffer information of this node.")
@Path("/system/buffers")
public class BuffersResource extends RestResource {
    private final Configuration configuration;
    private final ProcessBuffer processBuffer;

    @Inject
    public BuffersResource(Configuration configuration, ProcessBuffer processBuffer) {
        this.configuration = configuration;
        this.processBuffer = processBuffer;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get current utilization of buffers and caches of this node.")
    @Produces(MediaType.APPLICATION_JSON)
    public BuffersUtilizationSummary utilization() {
        final int ringSize = configuration.getRingSize();
        final long inputSize = processBuffer.size();
        final long inputUtil = inputSize / ringSize * 100;

        return BuffersUtilizationSummary.create(RingSummary.create(SingleRingUtilization.create(inputSize, inputUtil)));
    }
}
