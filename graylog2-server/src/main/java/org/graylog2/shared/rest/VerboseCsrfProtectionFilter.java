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

import org.glassfish.jersey.server.filter.CsrfProtectionFilter;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

public class VerboseCsrfProtectionFilter extends CsrfProtectionFilter {
    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        try {
            // Backward compatibility for Sidecars < 0.1.7
            if (!rc.getHeaders().containsKey("X-Graylog-Collector-Version")) {
                super.filter(rc);
            }
        } catch (BadRequestException badRequestException) {
            throw new BadRequestException(
                    "CSRF protection header is missing. Please add a \"" + HEADER_NAME + "\" header to your request.",
                    badRequestException
            );
        }
    }
}
