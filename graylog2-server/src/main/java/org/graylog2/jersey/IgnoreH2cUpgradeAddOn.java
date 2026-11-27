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
package org.graylog2.jersey;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.FilterChainEvent;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpEvents;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.util.Header;

import java.util.ArrayList;

public class IgnoreH2cUpgradeAddOn implements AddOn {
    @Override
    public void setup(NetworkListener listener, FilterChainBuilder builder) {
        builder.add(builder.indexOfType(HttpServerFilter.class) + 1, new BaseFilter() {
            @Override
            public NextAction handleEvent(FilterChainContext ctx, FilterChainEvent event) {
                if (event instanceof HttpEvents.IncomingHttpUpgradeEvent upgrade) {
                    final var request = upgrade.getHttpHeader();
                    final var otherOffers = new ArrayList<String>();
                    boolean offeredH2c = false;
                    for (final var value : request.getHeaders().values(Header.Upgrade)) {
                        for (final var token : value.split(",")) {
                            final var protocol = token.trim();
                            if ("h2c".equalsIgnoreCase(protocol)) {
                                offeredH2c = true;
                            } else if (!protocol.isEmpty()) {
                                otherOffers.add(protocol);
                            }
                        }
                    }
                    if (offeredH2c) {
                        request.getHeaders().removeHeader("HTTP2-Settings");
                        // Grizzly disabled HTTP body framing before firing this event.
                        request.setIgnoreContentModifiers(false);
                        if (otherOffers.isEmpty()) {
                            request.getHeaders().removeHeader(Header.Upgrade);
                            request.getUpgradeDC().recycle();
                            return ctx.getStopAction();
                        }
                        final var remaining = String.join(", ", otherOffers);
                        request.getHeaders().setValue(Header.Upgrade).setString(remaining);
                        request.setUpgrade(remaining);
                    }
                }
                return ctx.getInvokeAction();
            }
        });
    }
}
