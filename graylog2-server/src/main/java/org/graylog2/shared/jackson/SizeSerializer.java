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
package org.graylog2.shared.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.joschi.jadconfig.util.Size;

import java.io.IOException;

/**
 * Serializes JadConfig's Size utility object to bytes.
 */
public class SizeSerializer extends JsonSerializer<Size> {
    @Override
    public Class<Size> handledType() {
        return Size.class;
    }

    @Override
    public void serialize(Size value,
                          JsonGenerator jgen,
                          SerializerProvider provider) throws IOException {
        jgen.writeNumber(value.toBytes());
    }
}
