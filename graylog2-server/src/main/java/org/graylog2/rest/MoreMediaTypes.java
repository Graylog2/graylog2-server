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
package org.graylog2.rest;

import javax.ws.rs.core.MediaType;

public abstract class MoreMediaTypes {
    /**
     * A {@code String} constant representing {@value #APPLICATION_SCHEMA_JSON} media type.
     */
    public final static String APPLICATION_SCHEMA_JSON = "application/schema+json";
    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_SCHEMA_JSON} media type.
     *
     * @see <a href="http://json-schema.org/latest/json-schema-core.html">JSON Schema</a>
     */
    public final static MediaType APPLICATION_SCHEMA_JSON_TYPE = new MediaType("application", "schema+json");
    /**
     * A {@code String} constant representing {@value #APPLICATION_JAVASCRIPT} media type.
     */
    public final static String APPLICATION_JAVASCRIPT = "application/javascript";
    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_JAVASCRIPT} media type.
     *
     * @see <a href="https://tools.ietf.org/html/rfc4329#section-7.2">RFC 4329/a>
     */
    public final static MediaType APPLICATION_JAVASCRIPT_TYPE = new MediaType("application", "javascript");
    /**
     * A {@code String} constant representing {@value #TEXT_CSV} media type.
     */
    public final static String TEXT_CSV = "text/csv";
    /**
     * A {@link MediaType} constant representing {@value #TEXT_CSV} media type.
     */
    public final static MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");
    /**
     * A {@code String} constant representing {@value #APPLICATION_JSON} media type.
     */
    public final static String APPLICATION_JSON = "application/json";
    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_JSON} media type.
     */
    public final static MediaType APPLICATION_JSON_TYPE = new MediaType("application", "json");
    /**
     * A {@code String} constant representing {@value #APPLICATION_NDJSON} media type.
     */
    public final static String APPLICATION_NDJSON = "application/x-ndjson";
    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_NDJSON} media type.
     */
    public final static MediaType APPLICATION_NDJSON_TYPE = new MediaType("application", "x-ndjson");
    /**
     * A {@code String} constant representing {@value #APPLICATION_JSON} media type.
     */
    public final static String TEXT_PLAIN = "text/plain";
    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_JSON} media type.
     */
    public final static MediaType TEXT_PLAIN_TYPE = new MediaType("text", "plain");
}
