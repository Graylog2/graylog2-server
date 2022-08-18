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

import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nullable;

/**
 * This codec always returns a null Message.
 * Can be used in situations where we don't need a real codec or a message should be simply dropped after decoding.
 */
public class NullCodec implements Codec {
    public static final String NAME = "NullCodec";

    @Nullable
    @Override
    public Message decode(@NonNull RawMessage rawMessage) {
        return null;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    public Configuration getConfiguration() {
        return new Configuration(ImmutableMap.of());
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<NullCodec> {
        @Override
        NullCodec create(Configuration configuration);

        @Override
        NullCodec.Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
    }
}
