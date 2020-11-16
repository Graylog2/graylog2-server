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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.Date;

public final class MongoJodaDateTimeDeserializer extends StdScalarDeserializer<DateTime> {
    private static final DateTimeFormatter FORMATTER = ISODateTimeFormat.dateTime();

    public MongoJodaDateTimeDeserializer() {
        super(DateTime.class);
    }

    @Override
    public DateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        switch (jsonParser.currentToken()) {
            case VALUE_EMBEDDED_OBJECT:
                final Object embeddedObject = jsonParser.getEmbeddedObject();
                if (embeddedObject instanceof Date) {
                    final Date date = (Date) embeddedObject;
                    return new DateTime(date, DateTimeZone.UTC);
                } else {
                    throw new IllegalStateException("Unsupported token: " + jsonParser.currentToken());
                }
            case VALUE_STRING:
                final String text = jsonParser.getText();
                return DateTime.parse(text, FORMATTER).withZone(DateTimeZone.UTC);
            default:
                throw new IllegalStateException("Unsupported token: " + jsonParser.currentToken());
        }
    }
}