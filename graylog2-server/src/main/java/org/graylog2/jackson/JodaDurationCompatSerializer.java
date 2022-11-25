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
import com.fasterxml.jackson.datatype.joda.ser.DurationSerializer;
import org.joda.time.Duration;

import java.io.IOException;

/**
 * A Joda DurationSerializer that ignores the {@link com.fasterxml.jackson.databind.SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS}
 * setting and always serializes Durations to milliseconds.
 * This became necessary with the Jackson 2.13 update, which forced us to disable WRITE_DURATIONS_AS_TIMESTAMPS to
 * have {@link java.time.Duration} objects serialized as strings.
 */
public class JodaDurationCompatSerializer extends DurationSerializer {
    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // Always write as milliseconds, regardless of SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS
        gen.writeNumber(value.getMillis());
    }
}
