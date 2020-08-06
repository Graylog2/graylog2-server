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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;

public class EventsIndexMappingTest {

    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private static final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "5.0.0",
            "6.0.0",
            "7.0.0"
    })
    void createsValidMappingTemplates(String versionString) throws Exception {
        final Version version = Version.valueOf(versionString);
        final IndexMappingTemplate mapping = IndexMappingFactory.eventsIndexMappingFor(version);

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*"), at -> {
            at.jsonPathAsString("$.index_patterns").isEqualTo("test_*");
            at.jsonPathAsInteger("$.order").isEqualTo(-1);
            assertStandardMappingValues(at, version);
        });

        assertJsonPath(mapping.toTemplate(indexSetConfig, "test_*", 23), at -> {
            at.jsonPathAsString("$.index_patterns").isEqualTo("test_*");
            at.jsonPathAsInteger("$.order").isEqualTo(23);
            assertStandardMappingValues(at, version);
        });
    }

    private void assertJsonPath(Map<String, Object> map, Consumer<JsonPathAssert> consumer) throws Exception {
        AssertJsonPath.assertJsonPath(objectMapper.writeValueAsString(map), consumer);
    }

    private void assertStandardMappingValues(JsonPathAssert at, Version version) {
        at.jsonPathAsString("$.settings['index.refresh_interval']").isEqualTo("1s");

        at.jsonPathAsBoolean(keyFor("_source.enabled", version)).isTrue();
        at.jsonPathAsBoolean(keyFor("dynamic", version)).isFalse();

        at.jsonPathAsString(keyFor("dynamic_templates[0]fields.path_match", version)).isEqualTo("fields.*");
        at.jsonPathAsString(keyFor("dynamic_templates[0]fields.mapping.type", version)).isEqualTo("keyword");
        at.jsonPathAsBoolean(keyFor("dynamic_templates[0]fields.mapping.doc_values", version)).isTrue();
        at.jsonPathAsBoolean(keyFor("dynamic_templates[0]fields.mapping.index", version)).isTrue();

        at.jsonPathAsString(keyFor("properties.id.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.event_definition_type.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.event_definition_id.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.origin_context.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.timestamp.type", version)).isEqualTo("date");
        at.jsonPathAsString(keyFor("properties.timestamp.format", version)).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString(keyFor("properties.timestamp_processing.type", version)).isEqualTo("date");
        at.jsonPathAsString(keyFor("properties.timestamp_processing.format", version)).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString(keyFor("properties.timerange_start.type", version)).isEqualTo("date");
        at.jsonPathAsString(keyFor("properties.timerange_start.format", version)).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString(keyFor("properties.timerange_end.type", version)).isEqualTo("date");
        at.jsonPathAsString(keyFor("properties.timerange_end.format", version)).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        at.jsonPathAsString(keyFor("properties.streams.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.source_streams.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.message.type", version)).isEqualTo("text");
        at.jsonPathAsString(keyFor("properties.message.analyzer", version)).isEqualTo("standard");
        at.jsonPathAsBoolean(keyFor("properties.message.norms", version)).isFalse();
        at.jsonPathAsString(keyFor("properties.message.fields.keyword.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.source.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.key.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.key_tuple.type", version)).isEqualTo("keyword");
        at.jsonPathAsString(keyFor("properties.priority.type", version)).isEqualTo("long");
        at.jsonPathAsString(keyFor("properties.alert.type", version)).isEqualTo("boolean");
        at.jsonPathAsString(keyFor("properties.fields.type", version)).isEqualTo("object");
        at.jsonPathAsBoolean(keyFor("properties.fields.dynamic", version)).isTrue();
        at.jsonPathAsString(keyFor("properties.triggered_jobs.type", version)).isEqualTo("keyword");
    }

    private String keyFor(String keySuffix, Version version) {
        if (version.greaterThanOrEqualTo(Version.valueOf("7.0.0"))) {
            return "$.mappings." + keySuffix;
        }
        return "$.mappings.message." + keySuffix;
    }
}
