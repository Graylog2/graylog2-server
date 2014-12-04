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
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Path("/tools/natural_date_tester")
public class NaturalDateTesterResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RegexTesterResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> naturalDateTester(@QueryParam("string") @NotEmpty String string) {
        try {
            return new NaturalDateParser().parse(string).asMap();
        } catch (NaturalDateParser.DateNotParsableException e) {
            LOG.debug("Could not parse from natural date: " + string, e);
            throw new WebApplicationException(e, 422);
        }
    }
}
