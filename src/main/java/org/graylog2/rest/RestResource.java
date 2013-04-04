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

package org.graylog2.rest;

import javax.ws.rs.WebApplicationException;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RestResource {
	
	private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

	protected RestResource() { /* */ }
	
	protected ObjectId loadObjectId(String id) {
		try {
			return new ObjectId(id);
		} catch (IllegalArgumentException e) {
        	LOG.error("Invalid ObjectID \"" + id + "\". Returning HTTP 400.");
        	throw new WebApplicationException(400);
		}
	}
	
	protected String json(Object x) {
		return json(x, false);
	}
	
	protected String json(Object x, boolean prettyPrint) {
		Gson gson = new Gson();
		
        if (prettyPrint) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        
        return gson.toJson(x);
	}
	
}
