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
package org.graylog2.search;

import com.google.common.collect.ImmutableList;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.function.Function;

public class SearchQueryField {

    // We parse all date strings in UTC because we store and show all dates in UTC as well.
    private static final ImmutableList<DateTimeFormatter> DATE_TIME_FORMATTERS = ImmutableList.of(
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZoneUTC(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZoneUTC(),
            ISODateTimeFormat.dateTimeParser().withOffsetParsed().withZoneUTC()
    );

    private static DateTime parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return formatter.parseDateTime(value);
            } catch (IllegalArgumentException e) {
                // Try next one
            }
        }

        // It's probably not a date...
        throw new IllegalArgumentException("Unable to parse date: " + value);
    }

    public enum Type {

        STRING(value -> value),
        DATE(value -> SearchQueryField.parseDate(value)),
        INT(value -> Integer.parseInt(value)),
        LONG(value -> Long.parseLong(value)),
        OBJECT_ID(value -> new ObjectId(value)),
        BOOLEAN(value -> Boolean.parseBoolean(value));

        private final Function<String, Object> mongoValueConverter;

        Type(final Function<String, Object> mongoValueConverter) {
            this.mongoValueConverter = mongoValueConverter;
        }

        public Function<String, Object> getMongoValueConverter() {
            return mongoValueConverter;
        }
    }

    private final String dbField;
    private final Type fieldType;

    public static SearchQueryField create(String dbField) {
        return new SearchQueryField(dbField, Type.STRING);
    }

    public static SearchQueryField create(String dbField, Type fieldType) {
        return new SearchQueryField(dbField, fieldType);
    }

    public SearchQueryField(String dbField, Type fieldType) {
        this.dbField = dbField;
        this.fieldType = fieldType;
    }

    public String getDbField() {
        return dbField;
    }

    public Type getFieldType() {
        return fieldType;
    }
}
