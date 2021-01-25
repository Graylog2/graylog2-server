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
package org.graylog.grn;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Converts GRN strings format into a {@link GRN} object.
 */
public class GRNDeserializer extends StdDeserializer<GRN> {
    private final GRNRegistry grnRegistry;

    public GRNDeserializer(GRNRegistry grnRegistry) {
        super(GRN.class);
        this.grnRegistry = grnRegistry;
    }

    @Override
    public GRN deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return grnRegistry.parse(p.getValueAsString());
    }
}

