/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
