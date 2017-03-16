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