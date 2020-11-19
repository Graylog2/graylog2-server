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
package org.graylog.events.event;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.Date;

import static com.fasterxml.jackson.core.JsonToken.VALUE_EMBEDDED_OBJECT;
import static org.joda.time.DateTimeZone.UTC;

/**
 * Deserialize DateTime from MongoDB, ISODateTime or our ES_DATE_FORMAT format
 */
public class ESMongoDateTimeDeserializer extends StdScalarDeserializer<DateTime> {
    private static final JacksonJodaDateFormat ES_DATE_FORMAT =
            new JacksonJodaDateFormat(Tools.timeFormatterWithOptionalMilliseconds().withZone(UTC))
                    .withAdjustToContextTZOverride(true); // Overwrite global DeserializationFeature ADJUST_DATES_TO_CONTEXT_TIME_ZONE
    private static final JacksonJodaDateFormat ISO_DATE_FORMAT =
            new JacksonJodaDateFormat(ISODateTimeFormat.dateTimeParser().withZone(UTC))
                    .withAdjustToContextTZOverride(true); // Overwrite global DeserializationFeature ADJUST_DATES_TO_CONTEXT_TIME_ZONE
    private static final DateTimeDeserializer ES_DATE_DESERIALIZER = new DateTimeDeserializer(DateTime.class, ES_DATE_FORMAT);
    private static final DateTimeDeserializer ISO_DATE_DESERIALIZER = new DateTimeDeserializer(DateTime.class, ISO_DATE_FORMAT);


    public ESMongoDateTimeDeserializer() {
        super(DateTime.class);
    }

    @Override
    public DateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();

        // Values from MongoDB
        if (t == VALUE_EMBEDDED_OBJECT) {
            final Object embeddedObject = p.getEmbeddedObject();
            if (embeddedObject instanceof Date) {
                final Date date = (Date) embeddedObject;
                return new DateTime(date, UTC);
            } else {
                throw new IllegalStateException("Unsupported token: " + p.currentToken());
            }

        }

        try {
            // Values in ISO8601 format
            return (DateTime) ISO_DATE_DESERIALIZER.deserialize(p, ctxt);
        } catch (IllegalArgumentException  e) {
            // Values in Tools.ES_DATE_FORMAT format
            return (DateTime) ES_DATE_DESERIALIZER.deserialize(p, ctxt);
        }
    }
}
