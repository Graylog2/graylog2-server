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
package org.graylog2.inputs.codecs;

import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog2.inputs.transports.TcpTransport;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Codec(name = "gelf", displayName = "GELF")
public class GelfCodec extends AbstractCodec {
    private static final Logger log = LoggerFactory.getLogger(GelfCodec.class);
    private static final String CK_DECOMPRESS_SIZE_LIMIT = "decompress_size_limit";
    public static final int DEFAULT_DECOMPRESS_SIZE_LIMIT = 8388608;

    private final GelfChunkAggregator aggregator;
    private final GelfDecoder gelfDecoder;

    @Inject
    public GelfCodec(@Assisted Configuration configuration, GelfChunkAggregator aggregator, MessageFactory messageFactory) {
        super(configuration);
        this.aggregator = aggregator;
        this.gelfDecoder = new GelfDecoder(messageFactory,
                configuration.getInt(CK_DECOMPRESS_SIZE_LIMIT, DEFAULT_DECOMPRESS_SIZE_LIMIT),
                getCharsetOrDefault(configuration));
    }

    @Override
    public Optional<Message> decodeSafe(RawMessage rawMessage) {
        return gelfDecoder.decode(rawMessage);
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return aggregator;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<GelfCodec> {
        @Override
        GelfCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest requestedConfiguration = super.getRequestedConfiguration();
            requestedConfiguration.addField(new NumberField(
                    CK_DECOMPRESS_SIZE_LIMIT,
                    "Decompressed size limit",
                    DEFAULT_DECOMPRESS_SIZE_LIMIT,
                    "The maximum number of bytes after decompression.",
                    ConfigurationField.Optional.OPTIONAL));

            return requestedConfiguration;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(12201);
            }

            // GELF TCP always needs null-byte delimiter!
            if (cr.containsField(TcpTransport.CK_USE_NULL_DELIMITER)) {
                cr.getField(TcpTransport.CK_USE_NULL_DELIMITER).setDefaultValue(true);
            }
        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(GelfCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
