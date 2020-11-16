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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.github.zafarkhaja.semver.Version;

import java.io.IOException;

public class VersionDeserializer extends StdDeserializer<Version> {
    public VersionDeserializer() {
        super(Version.class);
    }

    @Override
    public Version deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        switch (p.getCurrentTokenId()) {
            case JsonTokenId.ID_STRING:
                final String str = p.getText().trim();
                return Version.valueOf(str);
            case JsonTokenId.ID_NUMBER_INT:
                return Version.forIntegers(p.getIntValue());
        }
        throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, "expected String or Number");
    }
}