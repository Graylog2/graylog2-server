/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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