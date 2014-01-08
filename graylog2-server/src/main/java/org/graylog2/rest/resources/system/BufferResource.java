/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Core;
import org.graylog2.plugin.buffers.BufferWatermark;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System/Buffers", description = "Buffer information of this node.")
@Path("/system/buffers")
public class BufferResource extends RestResource {

    @GET @Timed
    @ApiOperation(value = "Get current utilization of buffers and caches of this node.")
    @RequiresPermissions(RestPermissions.BUFFERS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public String utilization() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("buffers", buffers(core));
        result.put("master_caches", masterCaches(core));

        return json(result);
    }

    private Map<String, Object> masterCaches(Core core) {
        Map<String, Object> caches = Maps.newHashMap();
        Map<String, Object> input = Maps.newHashMap();
        Map<String, Object> output = Maps.newHashMap();

        input.put("size", core.getInputCache().size());
        output.put("size", core.getOutputCache().size());

        caches.put("input", input);
        caches.put("output", output);

        return caches;
    }

    private Map<String, Object> buffers(Core core) {
        Map<String, Object> buffers = Maps.newHashMap();
        Map<String, Object> input = Maps.newHashMap();
        Map<String, Object> output = Maps.newHashMap();

        int ringSize = core.getConfiguration().getRingSize();

        BufferWatermark pWm = new BufferWatermark(ringSize, core.processBufferWatermark());
        input.put("utilization_percent", pWm.getUtilizationPercentage());
        input.put("utilization", pWm.getUtilization());

        BufferWatermark oWm = new BufferWatermark(ringSize, core.outputBufferWatermark());
        output.put("utilization_percent", oWm.getUtilizationPercentage());
        output.put("utilization", oWm.getUtilization());

        buffers.put("input", input);
        buffers.put("output", output);

        return buffers;
    }


}
