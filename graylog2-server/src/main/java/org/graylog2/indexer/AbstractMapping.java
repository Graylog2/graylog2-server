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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;

public abstract class AbstractMapping implements IndexMappingTemplate {
    protected Map.Entry<String, ImmutableMap<String, Object>> timestampField() {
        return Map.entry(FIELD_TIMESTAMP, map()
                .put("type", "date")
                .put("format", dateFormat())
                .build());
    }

    protected ImmutableMap.Builder<String, Object> map() {
        return ImmutableMap.builder();
    }

    protected ImmutableList.Builder<Object> list() {
        return ImmutableList.builder();
    }

    protected String dateFormat() {
        return Constants.ES_DATE_FORMAT;
    }
}
