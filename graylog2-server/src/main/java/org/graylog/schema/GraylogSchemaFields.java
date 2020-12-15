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
package org.graylog.schema;

/**
 * Field names used in the standard Graylog Schema.
 *
 * @deprecated Please use the appropriate enums in this package rather than this collection of strings
 */
@Deprecated
public class GraylogSchemaFields {

    public static final String FIELD_TIMESTAMP = "timestamp";

    public static final String FIELD_ILLUMINATE_EVENT_CATEGORY = "gl2_event_category";
    public static final String FIELD_ILLUMINATE_EVENT_SUBCATEGORY = "gl2_event_subcategory";
    public static final String FIELD_ILLUMINATE_EVENT_TYPE = "gl2_event_type";
    public static final String FIELD_ILLUMINATE_EVENT_TYPE_CODE = "gl2_event_type_code";
    public static final String FIELD_ILLUMINATE_TAGS = "gl2_tags";
}
