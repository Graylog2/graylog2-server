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
package org.graylog2.rest.resources.tools;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.inputs.extractors.SplitAndIndexExtractor;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@RequiresAuthentication
@Path("/tools/split_and_index_tester")
public class SplitAndIndexTesterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SplitAndIndexTesterResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String splitAndIndexTester(@QueryParam("split_by") String splitBy, @QueryParam("index") int index, @QueryParam("string") String string) {
        if (string == null) {
            LOG.info("Missing parameters for split&index test.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        String cut = SplitAndIndexExtractor.cut(string, splitBy, index-1);
        int[] positions = SplitAndIndexExtractor.getCutIndices(string, splitBy, index-1);
        boolean successful = (cut != null);

        Map<String, Object> result = Maps.newHashMap();
        result.put("successful", successful);
        result.put("cut", cut);
        result.put("begin_index", positions[0]);
        result.put("end_index", positions[1]);

        return json(result);
    }

}
