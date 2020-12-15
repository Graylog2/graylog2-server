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
package org.graylog2.plugin.inputs.codecs;

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CodecAggregator {

    @Nonnull
    Result addChunk(ByteBuf buf);

    final class Result {
        private final ByteBuf message;
        private final boolean valid;

        public Result(@Nullable ByteBuf message, boolean valid) {
            this.message = message;
            this.valid = valid;
        }

        @Nullable
        public ByteBuf getMessage() {
            return message;
        }

        public boolean isValid() {
            return valid;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("message", message)
                .add("valid", valid)
                .toString();
        }
    }
}
