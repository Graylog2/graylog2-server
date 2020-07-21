/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.utilities.AssertJsonPath;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EventsIndexMappingTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IndexSetConfig indexSetConfig;

    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private void assertJsonPath(Map<String, Object> map, Consumer<JsonPathAssert> consumer) throws Exception {
        AssertJsonPath.assertJsonPath(objectMapper.writeValueAsString(map), consumer);
    }

    private void assertStandardMappingValues(JsonPathAssert at) {
        at.jsonPathAsString("$.settings['index.refresh_interval']").isEqualTo("1s");

        at.jsonPathAsBoolean("$.mappings.message._source.enabled").isTrue();
        at.jsonPathAsBoolean("$.mappings.message.dynamic").isFalse();

        at.jsonPathAsString("$.mappings.message.dynamic_templates[0]fields.path_match").isEqualTo("fields.*");
        at.jsonPathAsString("$.mappings.message.dynamic_templates[0]fields.mapping.type").isEqualTo("keyword");
        at.jsonPathAsBoolean("$.mappings.message.dynamic_templates[0]fields.mapping.doc_values").isTrue();
        at.jsonPathAsBoolean("$.mappings.message.dynamic_templates[0]fields.mapping.index").isTrue();

        at.jsonPathAsString("$.mappings.message.properties.id.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.event_definition_type.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.event_definition_id.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.origin_context.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.timestamp.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.message.properties.timestamp.format").isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString("$.mappings.message.properties.timestamp_processing.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.message.properties.timestamp_processing.format").isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString("$.mappings.message.properties.timerange_start.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.message.properties.timerange_start.format").isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString("$.mappings.message.properties.timerange_end.type").isEqualTo("date");
        at.jsonPathAsString("$.mappings.message.properties.timerange_end.format").isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString("$.mappings.message.properties.streams.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.source_streams.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.message.type").isEqualTo("text");
        at.jsonPathAsString("$.mappings.message.properties.message.analyzer").isEqualTo("standard");
        at.jsonPathAsBoolean("$.mappings.message.properties.message.norms").isFalse();
        at.jsonPathAsString("$.mappings.message.properties.message.fields.keyword.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.source.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.key.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.key_tuple.type").isEqualTo("keyword");
        at.jsonPathAsString("$.mappings.message.properties.priority.type").isEqualTo("long");
        at.jsonPathAsString("$.mappings.message.properties.alert.type").isEqualTo("boolean");
        at.jsonPathAsString("$.mappings.message.properties.fields.type").isEqualTo("object");
        at.jsonPathAsBoolean("$.mappings.message.properties.fields.dynamic").isTrue();
        at.jsonPathAsString("$.mappings.message.properties.triggered_jobs.type").isEqualTo("keyword");
    }

    @Test
    public void templateWithES5() throws Exception {
        final IndexMappingTemplate mapping = IndexMappingFactory.eventsIndexMappingFor(Version.valueOf("5.0.0"));

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*"), at -> {
            at.jsonPathAsString("$.template").isEqualTo("test_*");
            at.jsonPathAsInteger("$.order").isEqualTo(-1);
            assertStandardMappingValues(at);
        });

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*", 23), at -> {
            at.jsonPathAsString("$.template").isEqualTo("test_*");
            at.jsonPathAsInteger("$.order").isEqualTo(23);
            assertStandardMappingValues(at);
        });
    }

    @Test
    public void templateWithES6() throws Exception {
        final IndexMappingTemplate mapping = IndexMappingFactory.eventsIndexMappingFor(Version.valueOf("6.0.0"));

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*"), at -> {
            at.jsonPathAsListOf("$.index_patterns", String.class).isEqualTo(Collections.singletonList("test_*"));
            at.jsonPathAsInteger("$.order").isEqualTo(-1);
            assertStandardMappingValues(at);
        });

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*", 42), at -> {
            at.jsonPathAsListOf("$.index_patterns", String.class).isEqualTo(Collections.singletonList("test_*"));
            at.jsonPathAsInteger("$.order").isEqualTo(42);
            assertStandardMappingValues(at);
        });
    }

    @Test
    public void templateWithUnsupportedESVersions() {
        final String indexPattern = "test_*";

        assertThatThrownBy(() -> IndexMappingFactory.eventsIndexMappingFor(Version.valueOf("8.0.0")).toTemplate(indexSetConfig, indexPattern))
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageContaining("Unsupported Elasticsearch version: 8.0.0");

        assertThatThrownBy(() -> IndexMappingFactory.eventsIndexMappingFor(Version.valueOf("2.4.0")).toTemplate(indexSetConfig, indexPattern))
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageContaining("Unsupported Elasticsearch version: 2.4.0");
    }
}
