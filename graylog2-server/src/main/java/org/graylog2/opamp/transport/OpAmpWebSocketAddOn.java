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

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketFilter;

import java.util.Objects;

/**
 * Custom AddOn that wraps Grizzly's WebSocketAddOn and positions the auth filter
 * before the WebSocketFilter so auth can reject requests with HTTP 401 before upgrade.
 */
public class OpAmpWebSocketAddOn implements AddOn {
    private final WebSocketAddOn delegate = new WebSocketAddOn();
    private final OpAmpWebSocketAuthFilter authFilter;

    public OpAmpWebSocketAddOn(OpAmpWebSocketAuthFilter authFilter) {
        this.authFilter = Objects.requireNonNull(authFilter, "authFilter must not be null");
    }

    @Override
    public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
        // Let the standard WebSocketAddOn set up first
        delegate.setup(networkListener, builder);

        // Insert our auth filter before the WebSocketFilter
        final int wsFilterIdx = builder.indexOfType(WebSocketFilter.class);
        if (wsFilterIdx < 0) {
            throw new IllegalStateException("WebSocketFilter not found in filter chain; cannot position auth filter");
        }
        builder.add(wsFilterIdx, authFilter);
    }
}
