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

/**
 * Authentication context for OpAMP connections.
 * <p>
 * Sealed interface enables type-safe dispatch in message handlers:
 * <pre>{@code
 * switch (authContext) {
 *     case Enrollment e -> handleEnrollment(message, e.fleetId());
 * }
 * }</pre>
 */
public sealed interface OpAmpAuthContext {

    String REQUEST_ATTRIBUTE = "opamp.auth.context";

    static Optional<OpAmpAuthContext> fromRequest(HttpRequestPacket request) {
        return Optional.ofNullable((OpAmpAuthContext) request.getAttribute(REQUEST_ATTRIBUTE));
    }

    /**
     * Context for agents authenticating with enrollment tokens.
     */
    record Enrollment(String fleetId) implements OpAmpAuthContext {}
}
