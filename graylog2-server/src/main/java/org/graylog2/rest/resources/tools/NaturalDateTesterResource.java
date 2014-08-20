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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.utilities.date.NaturalDateParser;
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
@Path("/tools/natural_date_tester")
public class NaturalDateTesterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegexTesterResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String naturalDateTester(@QueryParam("string") String string) {
        if (string == null || string.isEmpty()) {
            LOG.info("Missing parameters for natural language date test.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Map<String, String> result = null;
        try {
            result = new NaturalDateParser().parse(string).asMap();
        } catch (NaturalDateParser.DateNotParsableException e) {
            LOG.debug("Could not parse from natural date: " + string);
            throw new WebApplicationException(422);
        }

        return json(result);
    }

}
