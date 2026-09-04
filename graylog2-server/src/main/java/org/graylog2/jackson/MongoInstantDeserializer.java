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
package org.graylog2.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

public final class MongoInstantDeserializer extends StdScalarDeserializer<Instant> {
    public MongoInstantDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return switch (jsonParser.currentToken()) {
            case VALUE_EMBEDDED_OBJECT -> ((Date) jsonParser.getEmbeddedObject()).toInstant();
            case VALUE_STRING -> Instant.parse(jsonParser.getText());
            default -> throw new IllegalStateException("Unsupported token: " + jsonParser.currentToken());
        };
    }
}
