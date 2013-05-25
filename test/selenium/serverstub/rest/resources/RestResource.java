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
package selenium.serverstub.rest.resources;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RestResource {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected RestResource() { /* */ }

    protected String json(Object x, boolean prettyPrint) {
        try {
            if (prettyPrint) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(x);
            } else {
                return objectMapper.writeValueAsString(x);
            }
        } catch (JsonProcessingException e) {
            System.out.println(e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (IOException ioe) {
            System.out.println(ioe);
            throw new WebApplicationException(ioe, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}