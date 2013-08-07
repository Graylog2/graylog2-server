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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/tools/regex_tester")
public class RegexTesterResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegexTesterResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String regexTester(@QueryParam("regex") String regex, @QueryParam("string") String string) {
        if (string == null || regex == null || regex.isEmpty()) {
            LOG.info("Missing parameters for regex test.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(string);
        boolean matched = matcher.find();

        // Get the first matched group.
        Map<String,Object> match = Maps.newHashMap();
        if (matcher.groupCount() > 0) {
            match.put("match", matcher.group(1));
            match.put("start", matcher.start(1));
            match.put("end", matcher.end(1));
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("matched", matched);
        result.put("match", match);
        result.put("regex", regex);
        result.put("string", string);

        return json(result);
    }

}
