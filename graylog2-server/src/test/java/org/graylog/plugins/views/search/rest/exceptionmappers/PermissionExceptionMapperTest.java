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
package org.graylog.plugins.views.search.rest.exceptionmappers;

import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog2.plugin.rest.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionExceptionMapperTest {
    private PermissionExceptionMapper sut;

    @BeforeEach
    public void setUp() throws Exception {
        sut = new PermissionExceptionMapper();
    }

    @Test
    public void responseHasStatus403() {
        Response response = sut.toResponse(new PermissionException(""));

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    public void responseHasMessageFromException() {
        PermissionException exception = new PermissionException("a message to you rudy");

        Response response = sut.toResponse(exception);

        assertThat(((ApiError) response.getEntity()).message()).isEqualTo(exception.getMessage());
    }
}
