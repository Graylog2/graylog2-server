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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.plugin.rest.CodedBadRequestException;
import org.graylog2.plugin.rest.CodedNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionMappersCodedErrorTest {
    @Test
    void badRequestMapperKeepsCodeAndParams() {
        final Response response = new BadRequestExceptionMapper()
                .toResponse(new CodedBadRequestException("a.b", "English", Map.of("x", "y")));

        final ApiError error = (ApiError) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(error.message()).isEqualTo("English");
        assertThat(error.code()).isEqualTo("a.b");
        assertThat(error.params()).containsEntry("x", "y");
    }

    @Test
    void badRequestMapperWithoutCodeIsUnchanged() {
        final Response response = new BadRequestExceptionMapper().toResponse(new BadRequestException("English"));

        final ApiError error = (ApiError) response.getEntity();
        assertThat(error.message()).isEqualTo("English");
        assertThat(error.code()).isNull();
    }

    @Test
    void webApplicationMapperKeepsCodeAndStatus() {
        final Response response = new WebApplicationExceptionMapper()
                .toResponse(new CodedNotFoundException("a.missing", "Not here", Map.of("id", "1")));

        final ApiError error = (ApiError) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(error.code()).isEqualTo("a.missing");
        assertThat(error.params()).containsEntry("id", "1");
    }

    @Test
    void webApplicationMapperWithoutCodeIsUnchanged() {
        final Response response = new WebApplicationExceptionMapper().toResponse(new NotFoundException("Not here"));

        final ApiError error = (ApiError) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(error.code()).isNull();
    }
}
