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
package org.graylog2.shared.rest.exceptionmappers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.server.ParamException;
import org.graylog2.plugin.rest.ApiError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParamExceptionMapperTest {
    private final ExceptionMapper<ParamException> mapper = new ParamExceptionMapper();

    @Test
    void mapsQueryParamExceptionToBadRequest() {
        final ParamException exception = new ParamException.QueryParamException(
                new NumberFormatException("For input string: \"hoge\""), "skip", "0");
        final Response response = mapper.toResponse(exception);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getEntity()).isInstanceOf(ApiError.class);
        assertThat(((ApiError) response.getEntity()).message())
                .isEqualTo("Invalid value for QueryParam parameter skip");
    }

    @Test
    void mapsLimitQueryParamExceptionToBadRequest() {
        final ParamException exception = new ParamException.QueryParamException(
                new NumberFormatException("For input string: \"hoge\""), "limit", "0");
        final Response response = mapper.toResponse(exception);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(((ApiError) response.getEntity()).message())
                .isEqualTo("Invalid value for QueryParam parameter limit");
    }
}
