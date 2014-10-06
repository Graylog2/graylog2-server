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
package org.graylog2.rest.resources.tools;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.inputs.extractors.SplitAndIndexExtractor;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Path("/tools/split_and_index_tester")
public class SplitAndIndexTesterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SplitAndIndexTesterResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> splitAndIndexTester(@QueryParam("split_by") @NotNull String splitBy,
                                                   @QueryParam("index") int index,
                                                   @QueryParam("string") @NotNull String string) {
        final String cut = SplitAndIndexExtractor.cut(string, splitBy, index - 1);
        int[] positions = SplitAndIndexExtractor.getCutIndices(string, splitBy, index - 1);

        final Map<String, Object> result = Maps.newHashMap();
        result.put("successful", cut != null);
        result.put("cut", cut);
        result.put("begin_index", positions[0]);
        result.put("end_index", positions[1]);

        return result;
    }

}
