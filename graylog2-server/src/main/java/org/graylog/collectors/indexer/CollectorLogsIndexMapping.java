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
import org.graylog.collectors.input.processor.CollectorLogRecordProcessor;
import org.graylog.schema.EventFields;
import org.graylog.schema.ServiceFields;
import org.graylog.schema.VendorFields;
import org.graylog2.indexer.ConstantsES7;
import org.graylog2.indexer.indexset.IndexSetMappingTemplate;
import org.graylog2.indexer.indices.Template;
import org.graylog2.indexer.template.AbstractMapping;
import org.graylog2.plugin.Message;

import java.util.Map;

/**
 * Index mapping for the collector self-logs index set.
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
                // Processing metadata fields, typed like in the default message template. Without explicit
                // mappings, the timestamps would be dynamically mapped as keyword (no range queries) and
                // the gl2_second_sort_field alias used by the search UI for stable sorting would not exist.
                .put(Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE, map().put("type", "long").build())
                .put(Message.FIELD_GL2_INPUT_MESSAGE_SIZE, map().put("type", "long").build())
                .put(Message.FIELD_GL2_RECEIVE_TIMESTAMP, map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                .put(Message.FIELD_GL2_ORIGINAL_TIMESTAMP, map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                .put(Message.FIELD_GL2_PROCESSING_TIMESTAMP, map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                .put(Message.FIELD_GL2_PROCESSING_DURATION_MS, map().put("type", "integer").build())
                .put(Message.FIELD_GL2_MESSAGE_ID, map().put("type", "keyword").build())
                .put(Message.GL2_SECOND_SORT_FIELD, map()
                        .put("type", "alias")
                        .put("path", Message.FIELD_GL2_MESSAGE_ID)
                        .build())
                // Collector identification fields
                .put(CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE, map().put("type", "keyword").build())
                .put(CollectorIngestCodec.FIELD_AGENT_ID, map().put("type", "keyword").build())
                .put(CollectorIngestCodec.FIELD_AGENT_SOURCE_ID, map().put("type", "keyword").build())
                .put(CollectorIngestCodec.FIELD_AGENT_FLEET_ID, map().put("type", "keyword").build())
                // Severity fields
                .put(VendorFields.VENDOR_EVENT_SEVERITY, map().put("type", "keyword").build())
                .put(VendorFields.VENDOR_EVENT_SEVERITY_LEVEL, map().put("type", "long").build())
                // Timestamp fields
                .put(EventFields.EVENT_CREATED, map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                .put(EventFields.EVENT_RECEIVED_TIME, map()
                        .put("type", "date")
                        .put("format", dateFormat())
                        .build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_CERT_FINGERPRINT, map().put("type", "keyword").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_COMPONENT_ID, map().put("type", "keyword").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_COMPONENT_KIND, map().put("type", "keyword").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_CRASH_COUNT, map().put("type", "long").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_DROPPED_RECORDS, map().put("type", "long").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_ENDPOINT, map().put("type", "keyword").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_EXIT_CODE, map().put("type", "long").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_PATH, map().put("type", "keyword").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_REJECTED_RECORDS, map().put("type", "long").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_RETRY_INTERVAL, map().put("type", "keyword").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_SCOPE, map().put("type", "keyword").build())
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_STATUS, map().put("type", "keyword").build())
                .put(EventFields.EVENT_COMPONENT, map().put("type", "keyword").build())
                .put(EventFields.EVENT_ERROR_DESCRIPTION, map().put("type", "keyword").build())
                .put(EventFields.EVENT_SEQUENCE, map().put("type", "long").build())
                .put(ServiceFields.SERVICE_NAME, map().put("type", "keyword").build())
                .put(ServiceFields.SERVICE_VERSION, map().put("type", "keyword").build())
                // All unpromoted log record attributes as an unparsed object
                .put(CollectorLogRecordProcessor.FIELD_COLLECTOR_LOG_ATTRIBUTES, map().put("enabled", false).build())
                .build();
    }

    @Override
    protected String dateFormat() {
        return ConstantsES7.ES_DATE_FORMAT;
    }
}
