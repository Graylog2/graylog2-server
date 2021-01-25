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
package org.graylog2.shared.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Range;

import java.io.IOException;

public class RangeJsonSerializer extends JsonSerializer<Range> {
    @Override
    public Class<Range> handledType() {
        return Range.class;
    }

    @Override
    public void serialize(Range range, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        final Integer lower = (Integer) range.lowerEndpoint();
        final Integer upper = (Integer) range.upperEndpoint();
        jgen.writeNumberField("start", lower);
        jgen.writeNumberField("length", upper - lower);
        jgen.writeEndObject();
    }
}
