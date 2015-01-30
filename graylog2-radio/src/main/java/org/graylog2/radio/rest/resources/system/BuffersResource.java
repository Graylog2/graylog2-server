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
import com.google.common.collect.Maps;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.rest.resources.RestResource;
import org.graylog2.shared.buffers.ProcessBuffer;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/buffers")
public class BuffersResource extends RestResource {
    private final Configuration configuration;
    private final InputBuffer inputBuffer;
    private final ProcessBuffer processBuffer;

    @Inject
    public BuffersResource(Configuration configuration, InputBuffer inputBuffer, ProcessBuffer processBuffer) {
        this.configuration = configuration;
        this.inputBuffer = inputBuffer;
        this.processBuffer = processBuffer;
    }

    @GET @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String utilization() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("buffers", buffers());

        return json(result);
    }

    private Map<String, Object> buffers() {
        Map<String, Object> buffers = Maps.newHashMap();
        Map<String, Object> input = Maps.newHashMap();
        Map<String, Object> process = Maps.newHashMap();

        final int inputBufferCapacity = configuration.getInputBufferRingSize();
        final long inputSize = inputBuffer.size();
        final float inputUtil = ((float) inputSize / inputBufferCapacity) * 100;
        input.put("utilization_percent", inputUtil);
        input.put("utilization", inputSize);

        buffers.put("input", input);

        final int processBufferCapacity = configuration.getRingSize();
        final long processSize = processBuffer.size();
        final float processUtil = ((float) processSize / processBufferCapacity) * 100;

        process.put("utilization_percent", processUtil);
        process.put("utilization", processSize);

        buffers.put("process", process);

        return buffers;
    }

}
