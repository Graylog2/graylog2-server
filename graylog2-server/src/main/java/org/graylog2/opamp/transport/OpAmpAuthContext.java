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
package org.graylog2.opamp.transport;

import org.glassfish.grizzly.http.HttpRequestPacket;

import java.util.Optional;

// TODO: the authenticated field is just a dummy and should be replaced with real auth context info later
public record OpAmpAuthContext(boolean authenticated) {

    static final String REQUEST_ATTRIBUTE = "opamp.auth.context";

    public static Optional<OpAmpAuthContext> fromRequest(HttpRequestPacket request) {
        return Optional.ofNullable((OpAmpAuthContext) request.getAttribute(REQUEST_ATTRIBUTE));
    }
}
