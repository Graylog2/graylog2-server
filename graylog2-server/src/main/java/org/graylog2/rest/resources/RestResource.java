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

package org.graylog2.rest.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RestResource {
	
	private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    protected final ObjectMapper objectMapper = new ObjectMapper();

	protected RestResource() { /* */ }
	
	protected ObjectId loadObjectId(String id) {
		try {
			return new ObjectId(id);
		} catch (IllegalArgumentException e) {
        	LOG.error("Invalid ObjectID \"" + id + "\". Returning HTTP 400.");
        	throw new WebApplicationException(400);
		}
	}

    protected String json(Object x, boolean prettyPrint) {
        try {
            if (prettyPrint) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(x);
            } else {
                return objectMapper.writeValueAsString(x);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Error while generating JSON", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected Map<String, Long> bytesToValueMap(long bytes) {
        Map<String, Long> r = Maps.newHashMap();

        int kb = 1024;
        int mb = kb*1024;

        r.put("bytes", bytes);
        r.put("kilobytes", bytes/kb);
        r.put("megabytes", bytes/mb);

        return r;
    }

}
