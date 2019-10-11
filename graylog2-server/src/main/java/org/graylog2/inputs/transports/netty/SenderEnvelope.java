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
