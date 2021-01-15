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
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

@Codec(name = "raw", displayName = "Raw String")
public class RawCodec extends AbstractCodec {

    @AssistedInject
    public RawCodec(@Assisted Configuration configuration) {
        super(configuration);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage raw) {
        return new Message(new String(raw.getPayload(), StandardCharsets.UTF_8), null, raw.getTimestamp());
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<RawCodec> {
        @Override
        RawCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(5555);
            }
        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(RawCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
