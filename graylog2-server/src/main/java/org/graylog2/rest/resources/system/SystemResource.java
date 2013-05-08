/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.Core;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.graylog2.Core;
import org.graylog2.rest.RestResource;

import com.google.common.collect.Maps;
import com.sun.jersey.api.core.ResourceConfig;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system")
public class SystemResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SystemResource.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context ResourceConfig rc;

    @GET @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public String system(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("facility", "graylog2-server");
        result.put("codename", Core.GRAYLOG2_CODENAME);
        result.put("server_id", core.getServerId());
       	result.put("version", Core.GRAYLOG2_VERSION);
        result.put("started_at", core.getStartedAt().toString());

        return json(result, prettyPrint);
    }

    @GET @Path("/fields")
    @Produces(MediaType.APPLICATION_JSON)
    public String analyze(@QueryParam("pretty") boolean prettyPrint) {
        Core core = (Core) rc.getProperty("core");

        Map<String, Object> result = Maps.newHashMap();
        result.put("fields", core.getIndexer().getAllMessageFields());

        return json(result, prettyPrint);
    }
}
