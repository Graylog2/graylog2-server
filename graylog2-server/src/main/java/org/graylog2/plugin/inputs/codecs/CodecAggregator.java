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
package org.graylog2.plugin.inputs.codecs;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;

public interface CodecAggregator {

    /**
     * Deprecated in favor of {@link RemoteAddressCodecAggregator#addChunk(ChannelBuffer, SocketAddress)} which will replace
     * this method in 3.0.
     *
     * @param buf the buffer containing a message part
     * @return the result of the aggregation
     */
    @Nonnull
    @Deprecated
    Result addChunk(ChannelBuffer buf);

    final class Result {
        private final ChannelBuffer message;
        private final boolean valid;

        public Result(@Nullable ChannelBuffer message, boolean valid) {
            this.message = message;
            this.valid = valid;
        }

        @Nullable
        public ChannelBuffer getMessage() {
            return message;
        }

        public boolean isValid() {
            return valid;
        }

        public static Result discard() {
            return new Result(null, false);
        }

        public static Result incomplete() {
            return new Result(null, true);
        }

        public static Result complete(ChannelBuffer buf) {
            return new Result(buf, true);
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
