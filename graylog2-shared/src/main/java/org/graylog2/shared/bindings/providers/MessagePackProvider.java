/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.shared.bindings.providers;

import org.graylog2.plugin.RadioMessage;
import org.msgpack.MessagePack;

import javax.inject.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * MessagePack generates classes for each new instance and message type which can leak into PermGen.
 *
 * Must be a singleton provider!
 */
public class MessagePackProvider implements Provider<MessagePack> {
    @Override
    public MessagePack get() {
        final MessagePack messagePack = new MessagePack();

        try {
            // Eagerly generate RadioMessage classes in the MessagePack object to avoid doing it on runtime.
            // The generated code is thread-safe, but generating it is not.
            final RadioMessage radioMessage = new RadioMessage();
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            messagePack.write(stream, radioMessage);
            final byte[] bytes = stream.toByteArray();
            messagePack.read(bytes, RadioMessage.class);
        } catch (IOException ignore) {
        }

        return messagePack;
    }
}
