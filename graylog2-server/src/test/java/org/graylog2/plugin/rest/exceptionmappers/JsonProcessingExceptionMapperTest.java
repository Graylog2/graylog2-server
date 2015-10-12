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

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.inject.Module;
import org.assertj.core.util.Lists;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonProcessingExceptionMapperTest {
    @Before
    public void setUpInjector() throws Exception {
        // The list of modules is empty for now so only JIT injection will be used.
        final List<Module> modules = Lists.emptyList();
        GuiceInjectorHolder.createInjector(modules);
    }

    @Test
    public void testToResponse() throws Exception {
        ExceptionMapper<JsonProcessingException> mapper = new JsonProcessingExceptionMapper();

        Response response = mapper.toResponse(new JsonMappingException("Boom!", JsonLocation.NA, new RuntimeException("rootCause")));

        assertEquals(response.getStatusInfo(), Response.Status.BAD_REQUEST);
        assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON_TYPE);
        assertTrue(response.hasEntity());
        assertTrue(response.getEntity() instanceof ApiError);

        final ApiError responseEntity = (ApiError)response.getEntity();
        assertTrue(responseEntity.message.startsWith("Boom!"));
    }
}
