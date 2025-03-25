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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.threeten.extra.PeriodDuration;

import java.io.IOException;

public class PeriodDurationSerializer extends StdSerializer<PeriodDuration> {

    public PeriodDurationSerializer() {
        super(PeriodDuration.class);
    }

    @Override
    public void serialize(PeriodDuration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        final String stringified = value.toString();
        gen.writeString(stringified);
    }
}
