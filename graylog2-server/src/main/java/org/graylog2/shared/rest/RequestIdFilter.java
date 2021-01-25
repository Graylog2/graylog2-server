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
package org.graylog2.shared.rest;

import com.google.common.base.Strings;
import org.graylog.util.uuid.ConcurrentUUID;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

// Needs to run before ShiroAuthorizationFilter
@Priority(Priorities.AUTHORIZATION - 20)
public class RequestIdFilter implements ContainerRequestFilter {
    public final static String X_REQUEST_ID = "X-Request-Id";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String id = requestContext.getHeaderString(X_REQUEST_ID);
        if (Strings.isNullOrEmpty(id)) {
            id = ConcurrentUUID.generateRandomUuid().toString();
        }
        requestContext.getHeaders().putSingle(X_REQUEST_ID, id);
    }
}
