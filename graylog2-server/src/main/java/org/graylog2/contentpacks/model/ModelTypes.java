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
package org.graylog2.contentpacks.model;

public interface ModelTypes {
    ModelType SIDECAR_COLLECTOR_CONFIGURATION_V1 = ModelType.of("sidecar_collector_configuration", "1");
    ModelType SIDECAR_COLLECTOR_V1 = ModelType.of("sidecar_collector", "1");
    ModelType GROK_PATTERN_V1 = ModelType.of("grok_pattern", "1");
    ModelType LOOKUP_ADAPTER_V1 = ModelType.of("lookup_adapter", "1");
    ModelType LOOKUP_CACHE_V1 = ModelType.of("lookup_cache", "1");
    ModelType LOOKUP_TABLE_V1 = ModelType.of("lookup_table", "1");
    ModelType INPUT_V1 = ModelType.of("input", "1");
    ModelType OUTPUT_V1 = ModelType.of("output", "1");
    ModelType PIPELINE_V1 = ModelType.of("pipeline", "1");
    ModelType PIPELINE_RULE_V1 = ModelType.of("pipeline_rule", "1");
    ModelType ROOT = ModelType.of("virtual-root", "1");
    ModelType STREAM_V1 = ModelType.of("stream", "1");
    ModelType EVENT_DEFINITION_V1 = ModelType.of("event_definition", "1");
    ModelType NOTIFICATION_V1 = ModelType.of("notification", "1");
    ModelType DASHBOARD_V1 = ModelType.of("dashboard", "1");
    ModelType DASHBOARD_V2 = ModelType.of("dashboard", "2");
    ModelType SEARCH_V1 = ModelType.of("search", "1");
    ModelType URL_WHITELIST_ENTRY_V1 = ModelType.of("url_whitelist_entry", "1");
}
