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
package org.graylog2.indexer.template;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.Constants;
import org.graylog2.plugin.Message;

import java.util.Map;

public abstract class AbstractMapping implements IndexMappingTemplate {
    protected Map.Entry<String, ImmutableMap<String, Object>> timestampField() {
        return Map.entry(Message.FIELD_TIMESTAMP, map()
                .put("type", "date")
                .put("format", dateFormat())
                .build());
    }

    /**
     * Mappings for the processing metadata fields the server stamps on every message.
     */
    protected Map<String, Map<String, Object>> gl2ProcessingFields() {
        return ImmutableMap.of(
                Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE, typeLong(),
                Message.FIELD_GL2_INPUT_MESSAGE_SIZE, typeLong(),
                Message.FIELD_GL2_RECEIVE_TIMESTAMP, typeTimeWithMillis(),
                Message.FIELD_GL2_ORIGINAL_TIMESTAMP, typeTimeWithMillis(),
                Message.FIELD_GL2_PROCESSING_TIMESTAMP, typeTimeWithMillis(),
                Message.FIELD_GL2_PROCESSING_DURATION_MS, typeInteger(),
                Message.FIELD_GL2_MESSAGE_ID, notAnalyzedString(),
                Message.GL2_SECOND_SORT_FIELD, aliasTo(Message.FIELD_GL2_MESSAGE_ID)
        );
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

    protected Map<String, Object> typeLong() {
        return ImmutableMap.of("type", "long");
    }

    protected Map<String, Object> typeInteger() {
        return ImmutableMap.of("type", "integer");
    }

    protected Map<String, Object> typeTimeWithMillis() {
        return ImmutableMap.of(
                "type", "date",
                "format", dateFormat());
    }

    protected Map<String, Object> notAnalyzedString() {
        return ImmutableMap.of("type", "keyword");
    }

    protected Map<String, Object> aliasTo(String path) {
        return Map.of("type", "alias",
                "path", path);
    }
}
