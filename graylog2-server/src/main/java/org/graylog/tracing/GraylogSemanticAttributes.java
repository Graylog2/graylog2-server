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
package org.graylog.tracing;

import io.opentelemetry.api.common.AttributeKey;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

public final class GraylogSemanticAttributes {
    private GraylogSemanticAttributes() {
    }

    public static final AttributeKey<String> LOOKUP_TABLE_NAME = stringKey("org.graylog.lookup.table.name");

    public static final AttributeKey<String> LOOKUP_CACHE_NAME = stringKey("org.graylog.lookup.cache.name");

    public static final AttributeKey<String> LOOKUP_CACHE_TYPE = stringKey("org.graylog.lookup.cache.type");

    public static final AttributeKey<String> LOOKUP_DATA_ADAPTER_NAME =
            stringKey("org.graylog.lookup.data_adapter.name");

    public static final AttributeKey<String> LOOKUP_DATA_ADAPTER_TYPE =
            stringKey("org.graylog.lookup.data_adapter.type");
}
