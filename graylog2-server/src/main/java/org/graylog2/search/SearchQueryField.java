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

import org.bson.types.ObjectId;
import org.graylog2.utilities.date.MultiFormatDateParser;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class SearchQueryField {

    private static final MultiFormatDateParser dateParser = new MultiFormatDateParser();

    public enum Type {
        STRING(value -> value),
        DATE(value -> dateParser.parseDate(value)),
        INT(value -> Integer.parseInt(value)),
        LONG(value -> Long.parseLong(value)),
        OBJECT_ID(value -> new ObjectId(value)),
        BOOLEAN(value -> Boolean.parseBoolean(value));

        public static final Collection<Type> NUMERIC_TYPES = List.of(DATE, LONG, INT);

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
        return new SearchQueryField(dbField, fieldType != null ? fieldType : Type.STRING);
    }

    SearchQueryField(String dbField, Type fieldType) {
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
