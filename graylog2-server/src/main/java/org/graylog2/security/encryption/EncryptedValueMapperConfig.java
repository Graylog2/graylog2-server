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
package org.graylog2.security.encryption;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configures an {@link ObjectMapper} to enable database serialization for {@link EncryptedValue}.
 */
public class EncryptedValueMapperConfig {
    private static final String KEY = EncryptedValueMapperConfig.class.getCanonicalName();

    private enum Type {
        DATABASE
    }

    public static boolean isDatabase(DatabindContext ctx) {
        return Type.DATABASE.equals(ctx.getAttribute(KEY));
    }

    public static void enableDatabase(ObjectMapper objectMapper) {
        // The serializer and deserializer will switch modes depending on the attribute
        objectMapper
                .setConfig(objectMapper.getDeserializationConfig().withAttribute(KEY, Type.DATABASE))
                .setConfig(objectMapper.getSerializationConfig().withAttribute(KEY, Type.DATABASE));
    }
}
