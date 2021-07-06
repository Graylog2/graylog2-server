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
import org.graylog2.indexer.indexset.IndexSetConfig;

import java.util.Map;

import static org.graylog2.plugin.Message.FIELD_GL2_MESSAGE_ID;

public abstract class FailureIndexMapping implements IndexMappingTemplate {
    @Override
    public Map<String, Object> toTemplate(IndexSetConfig indexSetConfig, String indexPattern, int order) {
        final String indexPatternsField = "index_patterns";
        final String indexRefreshInterval = "1s"; // TODO: do we need to tweak this?

        return map()
                .put(indexPatternsField, indexPattern)
                .put("order", order)
                .put("settings", map()
                        .put("index.mapping.ignore_malformed", true)
                        //.put("index.refresh_interval", indexRefreshInterval)
                        .build())
                .put("mappings", buildMappings())
                .build();
    }

    protected ImmutableMap<String, Object> buildMappings() {
        return map()
                .put("dynamic", false)
                .put("properties", fieldProperties())
                .build();
    }

    protected ImmutableMap<String, Object> fieldProperties() {
        return map()
                .put(FIELD_GL2_MESSAGE_ID, map()
                        .put("type", "keyword")
                        .build())
                .put("message", map()
                        .put("type", "text")
                        .build())
                .put("timestamp", map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                .put("failure_type", map()
                        .put("type", "keyword")
                        .build())
                .put("error_string", map()
                        .put("type", "text")
                        .build())
                .put("streams", map()
                        .put("type", "keyword")
                        .build())
                .put("source", map()
                        .put("type", "keyword")
                        .build())
                .put("failed_message", map()
                        .put("type", "object")
                        .put("enabled", false)
                        .build())
                .build();
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
