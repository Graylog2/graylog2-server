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

public interface CodecAggregator {

    @Nonnull
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

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("message", message)
                .add("valid", valid)
                .toString();
        }
    }
}
