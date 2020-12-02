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

import com.google.inject.Inject;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;

import java.util.Map;

public class CodecFactory {
    private Map<String, Codec.Factory<? extends Codec>> codecFactory;

    @Inject
    public CodecFactory(Map<String, Codec.Factory<? extends Codec>> codecFactory) {
        this.codecFactory = codecFactory;
    }

    public Map<String, Codec.Factory<? extends Codec>> getFactory() {
        return codecFactory;
    }

    public Codec create(String type, Configuration configuration) {
        final Codec.Factory<? extends Codec> factory = this.codecFactory.get(type);

        if (factory == null) {
            throw new IllegalArgumentException("Codec type " + type + " does not exist.");
        }

        return factory.create(configuration);
    }
}
