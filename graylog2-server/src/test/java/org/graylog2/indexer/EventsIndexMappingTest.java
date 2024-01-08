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
import org.graylog.testing.jsonpath.JsonPathAssert;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.TemplateIndexSetConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.utilities.AssertJsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class EventsIndexMappingTest {

    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private static final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
    private static final TemplateIndexSetConfig templateIndexSetConfig = mock(TemplateIndexSetConfig.class);
    public static final String DATE_FORMAT = "uuuu-MM-dd HH:mm:ss.SSS";

    @ParameterizedTest
    @ValueSource(strings = {
            "7.0.0",
            "OpenSearch:1.2.3"
    })
    void createsValidMappingTemplates(final String versionString) throws Exception {
        final SearchVersion version = SearchVersion.decode(versionString);
        final IndexMappingTemplate mapping = new EventIndexTemplateProvider().create(version, indexSetConfig);
        doReturn("test_*").when(templateIndexSetConfig).indexWildcard();

        var template1 = mapping.toTemplate(templateIndexSetConfig);
        assertThat(template1.indexPatterns()).isEqualTo(List.of("test_*"));
        assertThat(template1.order()).isEqualTo(-1);
        assertJsonPath(template1.mappings(), this::assertStandardMappingValues);
        assertJsonPath(template1.settings(), this::assertStandardSettingsValues);

        var template2 = mapping.toTemplate(templateIndexSetConfig, 23L);
        assertThat(template2.indexPatterns()).isEqualTo(List.of("test_*"));
        assertThat(template2.order()).isEqualTo(23);
        assertJsonPath(template2.mappings(), this::assertStandardMappingValues);
        assertJsonPath(template2.settings(), this::assertStandardSettingsValues);
    }

    private void assertJsonPath(final Map<String, Object> map, final Consumer<JsonPathAssert> consumer) throws Exception {
        AssertJsonPath.assertJsonPath(objectMapper.writeValueAsString(map), consumer);
    }

    private void assertStandardSettingsValues(JsonPathAssert at) {
        at.jsonPathAsString("$.['index.refresh_interval']").isEqualTo("1s");
    }

    private void assertStandardMappingValues(JsonPathAssert at) {
        at.jsonPathAsBoolean("$._source.enabled").isTrue();
        at.jsonPathAsBoolean("$.dynamic").isFalse();

        at.jsonPathAsString("$.dynamic_templates[0]fields.path_match").isEqualTo("fields.*");
        at.jsonPathAsString("$.dynamic_templates[0]fields.mapping.type").isEqualTo("keyword");
        at.jsonPathAsBoolean("$.dynamic_templates[0]fields.mapping.doc_values").isTrue();
        at.jsonPathAsBoolean("$.dynamic_templates[0]fields.mapping.index").isTrue();

        at.jsonPathAsString("$.properties.id.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.event_definition_type.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.event_definition_id.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.origin_context.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.timestamp.type").isEqualTo("date");
        at.jsonPathAsString("$.properties.timestamp.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.properties.timestamp_processing.type").isEqualTo("date");
        at.jsonPathAsString("$.properties.timestamp_processing.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.properties.timerange_start.type").isEqualTo("date");
        at.jsonPathAsString("$.properties.timerange_start.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.properties.timerange_end.type").isEqualTo("date");
        at.jsonPathAsString("$.properties.timerange_end.format").isEqualTo(DATE_FORMAT);
        at.jsonPathAsString("$.properties.streams.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.source_streams.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.message.type").isEqualTo("text");
        at.jsonPathAsString("$.properties.message.analyzer").isEqualTo("standard");
        at.jsonPathAsBoolean("$.properties.message.norms").isFalse();
        at.jsonPathAsString("$.properties.message.fields.keyword.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.source.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.key.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.key_tuple.type").isEqualTo("keyword");
        at.jsonPathAsString("$.properties.priority.type").isEqualTo("long");
        at.jsonPathAsString("$.properties.alert.type").isEqualTo("boolean");
        at.jsonPathAsString("$.properties.fields.type").isEqualTo("object");
        at.jsonPathAsBoolean("$.properties.fields.dynamic").isTrue();
        at.jsonPathAsString("$.properties.triggered_jobs.type").isEqualTo("keyword");
    }
}
