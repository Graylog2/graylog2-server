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
package org.graylog.collectors.indexer;

import com.google.common.collect.ImmutableMap;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog2.indexer.ConstantsES7;
import org.graylog2.indexer.indexset.IndexSetMappingTemplate;
import org.graylog2.indexer.indices.Template;
import org.graylog2.indexer.template.AbstractMapping;

import java.util.Map;

/**
 * Index mapping for the collector self-logs index set.
 * <p>
 * TODO: Finalize the schema once we can inspect real collector self-log payloads.
 *  The current mapping is preliminary.
 */
public class CollectorLogsIndexMapping extends AbstractMapping {
    @Override
    public Template toTemplate(IndexSetMappingTemplate indexSetConfig, Long order) {
        final var mappings = new Template.Mappings(buildMappings());
        final var settings = new Template.Settings(Map.of("index.refresh_interval", "1s"));
        return Template.create(indexSetConfig.indexWildcard(), mappings, order, settings);
    }

    private ImmutableMap<String, Object> buildMappings() {
        return map()
                .put("_source", map().put("enabled", true).build())
                .put("dynamic", true)
                .put("dynamic_templates", list()
                        .add(map()
                                .put("strings_as_keyword", map()
                                        .put("match_mapping_type", "string")
                                        .put("mapping", map()
                                                .put("type", "keyword")
                                                .put("doc_values", true)
                                                .put("index", true)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .put("properties", fieldProperties())
                .build();
    }

    private ImmutableMap<String, Object> fieldProperties() {
        return map()
                // Standard message fields
                .put(timestampField())
                .put("message", map()
                        .put("type", "text")
                        .put("analyzer", "standard")
                        .put("norms", false)
                        .put("fields", map()
                                .put("keyword", map().put("type", "keyword").build())
                                .build())
                        .build())
                .put("source", map().put("type", "keyword").build())
                .put("streams", map().put("type", "keyword").build())
                // Collector identification fields
                .put(CollectorIngestCodec.FIELD_COLLECTOR_SOURCE_TYPE, map().put("type", "keyword").build())
                .put(CollectorIngestCodec.FIELD_COLLECTOR_INSTANCE_UID, map().put("type", "keyword").build())
                .put("gl2_source_collector", map().put("type", "keyword").build())
                // Severity fields
                .put("vendor_event_severity", map().put("type", "keyword").build())
                .put("vendor_event_severity_level", map().put("type", "long").build())
                // Timestamp fields
                .put("event_created", map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                .put("event_received_time", map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                // Collector-specific fields from CollectorLogRecordProcessor
                .put("collector_service_name", map().put("type", "keyword").build())
                .put("collector_service_version", map().put("type", "keyword").build())
                .build();
    }

    @Override
    protected String dateFormat() {
        return ConstantsES7.ES_DATE_FORMAT;
    }
}
