/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.rest.exceptionmappers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonProcessingExceptionMapperTest {
    @BeforeClass
    public static void setUpInjector() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Test
    public void testToResponse() throws Exception {
        final ExceptionMapper<JsonProcessingException> mapper = new JsonProcessingExceptionMapper();
        final JsonParser jsonParser = new JsonFactory().createParser("");
        final JsonMappingException exception = new JsonMappingException(jsonParser, "Boom!", new RuntimeException("rootCause"));
        final Response response = mapper.toResponse(exception);

        assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
        assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON_TYPE);
        assertTrue(response.hasEntity());
        assertTrue(response.getEntity() instanceof ApiError);

        final ApiError responseEntity = (ApiError) response.getEntity();
        assertTrue(responseEntity.message().startsWith("Boom!"));
    }
}
