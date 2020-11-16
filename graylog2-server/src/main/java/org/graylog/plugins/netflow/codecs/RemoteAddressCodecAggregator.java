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
package org.graylog.plugins.netflow.codecs;

import io.netty.buffer.ByteBuf;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;

public interface RemoteAddressCodecAggregator extends CodecAggregator {

    @Nonnull
    @Override
    default Result addChunk(ByteBuf buf) {
        return addChunk(buf, null);
    }

    @Nonnull
    Result addChunk(ByteBuf buf, @Nullable SocketAddress remoteAddress);
}
