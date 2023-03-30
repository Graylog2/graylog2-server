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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.utilities.AssertJsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;

public class EventsIndexMappingTest {

    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private static final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
    public static final String DATE_FORMAT = "uuuu-MM-dd HH:mm:ss.SSS";

    @ParameterizedTest
    @ValueSource(strings = {
            "7.0.0",
            "OpenSearch:1.2.3"
    })
    void createsValidMappingTemplates(final String versionString) throws Exception {
        final SearchVersion version = SearchVersion.decode(versionString);
        final IndexMappingTemplate mapping = new EventIndexTemplateProvider().create(version, Mockito.mock(IndexSetConfig.class));

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*"), at -> {
            at.jsonPathAsString("$.index_patterns").isEqualTo("test_*");
            at.jsonPathAsInteger("$.order").isEqualTo(-1);
            assertStandardMappingValues(at);
        });

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*", 23), at -> {
            at.jsonPathAsString("$.index_patterns").isEqualTo("test_*");
            at.jsonPathAsInteger("$.order").isEqualTo(23);
            assertStandardMappingValues(at);
        });
    }

    private void assertJsonPath(final Map<String, Object> map, final Consumer<JsonPathAssert> consumer) throws Exception {
        AssertJsonPath.assertJsonPath(objectMapper.writeValueAsString(map), consumer);
    }

    private void assertStandardMappingValues(JsonPathAssert at) {
        at.jsonPathAsString("$.settings['index.refresh_interval']").isEqualTo("1s");

        at.jsonPathAsBoolean("$.mappings._source.enabled").isTrue();
        at.jsonPathAsBoolean("$.mappings.dynamic").isFalse();

        at.jsonPathAsString("$.mappings.dynamic_templates[0]fields.path_match").isEqualTo("fields.*");
        at.jsonPathAsString("$.mappings.dynamic_templates[0]fields.mapping.type").isEqualTo("keyword");
        at.jsonPathAsBoolean("$.mappings.dynamic_templates[0]fields.mapping.doc_values").isTrue();
        at.jsonPathAsBoolean("$.mappings.dynamic_templates[0]fields.mapping.index").isTrue();

        at.jsonPathAsString("$.mappings.properties.id.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.event_definition_type.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.event_definition_id.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.origin_context.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.timestamp.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.properties.timestamp.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.mappings.properties.timestamp_processing.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.properties.timestamp_processing.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.mappings.properties.timerange_start.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.properties.timerange_start.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.mappings.properties.timerange_end.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.properties.timerange_end.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.mappings.properties.streams.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.source_streams.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.message.type").isEqualTo("text");
        at.jsonPathAsString("$.mappings.properties.message.analyzer").isEqualTo("standard");
        at.jsonPathAsBoolean("$.mappings.properties.message.norms").isFalse();
        at.jsonPathAsString("$.mappings.properties.message.fields.keyword.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.source.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.key.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.key_tuple.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.properties.priority.type").isEqualTo("long");
        at.jsonPathAsString("$.mappings.properties.alert.type").isEqualTo("boolean");
        at.jsonPathAsString("$.mappings.properties.fields.type").isEqualTo("object");
        at.jsonPathAsBoolean("$.mappings.properties.fields.dynamic").isTrue();
        at.jsonPathAsString("$.mappings.properties.triggered_jobs.type").isEqualTo("keyword");
    }
}
