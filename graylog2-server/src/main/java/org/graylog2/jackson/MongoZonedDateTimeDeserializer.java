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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

public final class MongoZonedDateTimeDeserializer extends StdScalarDeserializer<ZonedDateTime> {
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOffset("+HHmm", "Z")
            .toFormatter();

    public MongoZonedDateTimeDeserializer() {
        super(ZonedDateTime.class);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        switch (jsonParser.currentToken()) {
            case VALUE_EMBEDDED_OBJECT:
                final Object embeddedObject = jsonParser.getEmbeddedObject();
                if (embeddedObject instanceof Date) {
                    final Date date = (Date) embeddedObject;
                    return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
                } else {
                    throw new IllegalStateException("Unsupported token: " + jsonParser.currentToken());
                }
            case VALUE_STRING:
                final String text = jsonParser.getText();
                return ZonedDateTime.parse(text, FORMATTER).withZoneSameInstant(ZoneOffset.UTC);
            default:
                throw new IllegalStateException("Unsupported token: " + jsonParser.currentToken());
        }
    }
}