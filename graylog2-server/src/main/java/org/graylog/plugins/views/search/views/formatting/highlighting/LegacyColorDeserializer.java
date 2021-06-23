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
package org.graylog.plugins.views.search.views.formatting.highlighting;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import java.io.IOException;

public class LegacyColorDeserializer extends JsonDeserializer<HighlightingColor> {
    @Override
    public HighlightingColor deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final JsonToken token = p.getCurrentToken();
        if (token.isScalarValue()) {
            return StaticColor.create(token.asString());
        }

        return p.getCodec().readValue(p, HighlightingColor.class);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        final JsonToken token = p.getCurrentToken();
        if (token.isScalarValue()) {
            final String color = p.getCodec().readValue(p, String.class);
            return StaticColor.create(color);
        }

        return super.deserializeWithType(p, ctxt, typeDeserializer);
    }
}
