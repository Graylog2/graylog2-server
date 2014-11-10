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
package org.graylog2.inputs.codecs;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RadioMessage;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class RadioMessageCodec implements Codec {
    private static final Logger log = LoggerFactory.getLogger(RadioMessageCodec.class);

    private final MessagePack messagePack;

    @AssistedInject
    public RadioMessageCodec(@Assisted Configuration configuration, MessagePack messagePack) {
        this.messagePack = messagePack;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            final RadioMessage msg = messagePack.read(rawMessage.getPayload(), RadioMessage.class);

            if (!msg.strings.containsKey("message") || !msg.strings.containsKey("source") || msg.timestamp <= 0) {
                log.error("Incomplete AMQP message. Skipping.");
                return null;
            }

            final Message event = new Message(
                    msg.strings.get("message"),
                    msg.strings.get("source"),
                    new DateTime(msg.timestamp, DateTimeZone.UTC)
            );

            event.addStringFields(msg.strings);
            event.addLongFields(msg.longs);
            event.addDoubleFields(msg.doubles);

            return event;
        } catch (IOException e) {
            log.error("Unable to unpack radio message");
        }
        return null;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return "Radio Message";
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<RadioMessageCodec> {
        @Override
        public RadioMessageCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

}
