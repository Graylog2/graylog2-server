/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Range;

import java.io.IOException;

class RangeJsonSerializer extends JsonSerializer<Range> {
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
