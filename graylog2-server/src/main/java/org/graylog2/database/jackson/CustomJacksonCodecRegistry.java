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
package org.graylog2.database.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.mongojack.JacksonCodecRegistry;
import org.mongojack.internal.stream.JacksonDecoder;
import org.mongojack.internal.stream.JacksonEncoder;

import java.util.concurrent.ConcurrentHashMap;

import static org.bson.UuidRepresentation.UNSPECIFIED;

/**
 * A custom implementation of Mongojack's {@link JacksonCodecRegistry} so that we are able to register our
 * {@link CustomJacksonCodec}.
 */
public class CustomJacksonCodecRegistry extends JacksonCodecRegistry {
    protected final ConcurrentHashMap<Class<?>, Codec<?>> codecCache = new ConcurrentHashMap<>();
    protected final ObjectMapper objectMapper;

    public CustomJacksonCodecRegistry(ObjectMapper objectMapper, CodecRegistry defaultCodecRegistry) {
        super(objectMapper, defaultCodecRegistry, null, UNSPECIFIED);
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Codec<T> addCodecForClass(Class<T> clazz) {
        return (Codec<T>) codecCache.computeIfAbsent(clazz, (k) -> {
            JacksonEncoder<T> encoder = new JacksonEncoder<>(clazz, null, objectMapper, UNSPECIFIED);
            JacksonDecoder<T> decoder = new JacksonDecoder<>(clazz, null, objectMapper, UNSPECIFIED);
            return new CustomJacksonCodec<>(encoder, decoder, objectMapper, this);
        });
    }
}
