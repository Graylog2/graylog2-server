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
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupCacheKeyTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void serialize() {
        final LookupCacheKey cacheKey = LookupCacheKey.createFromJSON("prefix", "key");
        final JsonNode node = objectMapper.convertValue(cacheKey, JsonNode.class);
        assertThat(node.isObject()).isTrue();
        assertThat(node.fieldNames()).containsExactly("prefix", "key");
        assertThat(node.path("prefix").isTextual()).isTrue();
        assertThat(node.path("prefix").asText()).isEqualTo("prefix");
        assertThat(node.path("key").isTextual()).isTrue();
        assertThat(node.path("key").asText()).isEqualTo("key");
    }

    @Test
    public void serializePrefixOnly() {
        final LookupCacheKey cacheKey = LookupCacheKey.createFromJSON("prefix", null);
        final JsonNode node = objectMapper.convertValue(cacheKey, JsonNode.class);
        assertThat(node.isObject()).isTrue();
        assertThat(node.fieldNames()).containsExactly("prefix", "key");
        assertThat(node.path("prefix").isTextual()).isTrue();
        assertThat(node.path("prefix").asText()).isEqualTo("prefix");
        assertThat(node.path("key").isNull()).isTrue();
    }

    @Test
    public void deserialize() throws IOException {
        final String json = "{\"prefix\":\"prefix\", \"key\":\"key\"}";
        final LookupCacheKey cacheKey = objectMapper.readValue(json, LookupCacheKey.class);
        assertThat(cacheKey.prefix()).isEqualTo("prefix");
        assertThat(cacheKey.key()).isEqualTo("key");
        assertThat(cacheKey.isPrefixOnly()).isFalse();
    }

    @Test
    public void deserializePrefixOnly() throws IOException {
        final String json = "{\"prefix\":\"prefix\"}";
        final LookupCacheKey cacheKey = objectMapper.readValue(json, LookupCacheKey.class);
        assertThat(cacheKey.prefix()).isEqualTo("prefix");
        assertThat(cacheKey.key()).isNull();
        assertThat(cacheKey.isPrefixOnly()).isTrue();
    }

}