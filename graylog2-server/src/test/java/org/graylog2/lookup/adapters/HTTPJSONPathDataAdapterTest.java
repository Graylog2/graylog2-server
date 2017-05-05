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
package org.graylog2.lookup.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class HTTPJSONPathDataAdapterTest {
    private static Map<Object, Object> JSON = ImmutableMap.of(
            "hello", "world",
            "map", ImmutableMap.of("key1", "value1", "key2", "value2"),
            "list", ImmutableList.of("a", "b", "c")
    );

    private InputStream body;
    private InputStream emptyBody;

    @Before
    public void setUp() throws Exception {
        this.body = new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(JSON));
        this.emptyBody = new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(Collections.emptyMap()));
    }

    @Test
    public void parseBodyWithMapMultiValue() throws Exception {
        final JsonPath singlePath = JsonPath.compile("$.hello");
        final JsonPath multiPath = JsonPath.compile("$.map");
        final LookupResult result = HTTPJSONPathDataAdapter.parseBody(singlePath, multiPath, body);

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.singleValue()).isEqualTo("world");

        assertThat(result.multiValue()).isNotNull();
        assertThat(result.multiValue()).isInstanceOf(Map.class);
        assertThat(result.multiValue()).containsOnly(
                entry("key1", "value1"),
                entry("key2", "value2")
        );
    }

    @Test
    public void parseBodyWithListMultiValue() throws Exception {
        final JsonPath singlePath = JsonPath.compile("$.hello");
        final JsonPath multiPath = JsonPath.compile("$.list");
        final LookupResult result = HTTPJSONPathDataAdapter.parseBody(singlePath, multiPath, body);

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.singleValue()).isEqualTo("world");

        assertThat(result.multiValue()).isNotNull();
        assertThat(result.multiValue()).isInstanceOf(Map.class);
        assertThat(result.multiValue()).containsKey("value");
        //noinspection ConstantConditions
        assertThat(result.multiValue().get("value")).isInstanceOf(Collection.class);
        //noinspection unchecked,ConstantConditions
        assertThat((Collection) result.multiValue().get("value")).containsOnly("a", "b", "c");
    }

    @Test
    public void parseEmptyBody() throws Exception {
        final JsonPath singlePath = JsonPath.compile("$.hello");
        final JsonPath multiPath = JsonPath.compile("$.list");
        final LookupResult result = HTTPJSONPathDataAdapter.parseBody(singlePath, multiPath, emptyBody);

        assertThat(result.isEmpty()).isTrue();
    }
}