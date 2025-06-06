/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin.rest.exceptionmappers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.graylog2.plugin.rest.RequestError;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
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

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.hasEntity()).isTrue();
        assertThat(response.getEntity()).isInstanceOf(RequestError.class);

        final var responseEntity = (RequestError) response.getEntity();
        assertTrue(responseEntity.message().startsWith("Boom!"));
    }
}
