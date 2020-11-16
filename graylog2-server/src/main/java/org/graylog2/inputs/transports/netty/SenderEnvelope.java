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
package org.graylog2.inputs.transports.netty;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.DefaultAddressedEnvelope;

import java.net.InetSocketAddress;

/**
 * Helper class to simplify envelope creation.
 */
public class SenderEnvelope {
    /**
     * Returns a {@link AddressedEnvelope} of the given message and message sender values.
     *
     * @param message the message
     * @param sender the sender address
     * @param <M> message type
     * @param <A> sender type
     * @return the envelope
     */
    public static <M, A extends InetSocketAddress> AddressedEnvelope<M, A> of(M message, A sender) {
        return new DefaultAddressedEnvelope<>(message, null, sender);
    }
}
