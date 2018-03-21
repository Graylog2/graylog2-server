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
package org.graylog2.rest.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog2.database.PaginatedList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class PaginatedResponseTest {
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        this.objectMapper = new ObjectMapperProvider(getClass().getClassLoader(), Collections.emptySet()).get();
    }

    @Test
    public void serialize() throws Exception {
        final ImmutableList<String> values = ImmutableList.of("hello", "world");
        final PaginatedList<String> paginatedList = new PaginatedList<>(values, values.size(), 1, 10);
        final PaginatedResponse<String> response = PaginatedResponse.create("foo", paginatedList);

        final DocumentContext ctx = JsonPath.parse(objectMapper.writeValueAsString(response));
        final JsonPathAssert jsonPathAssert = JsonPathAssert.assertThat(ctx);

        jsonPathAssert.jsonPathAsInteger("$.total").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.count").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.page").isEqualTo(1);
        jsonPathAssert.jsonPathAsInteger("$.per_page").isEqualTo(10);
        jsonPathAssert.jsonPathAsString("$.foo[0]").isEqualTo("hello");
        jsonPathAssert.jsonPathAsString("$.foo[1]").isEqualTo("world");
    }

    @Test
    public void serializeWithQuery() throws Exception {
        final ImmutableList<String> values = ImmutableList.of("hello", "world");
        final PaginatedList<String> paginatedList = new PaginatedList<>(values, values.size(), 1, 10);
        final PaginatedResponse<String> response = PaginatedResponse.create("foo", paginatedList, "query1");

        final DocumentContext ctx = JsonPath.parse(objectMapper.writeValueAsString(response));
        final JsonPathAssert jsonPathAssert = JsonPathAssert.assertThat(ctx);

        jsonPathAssert.jsonPathAsString("$.query").isEqualTo("query1");
        jsonPathAssert.jsonPathAsInteger("$.total").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.count").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.page").isEqualTo(1);
        jsonPathAssert.jsonPathAsInteger("$.per_page").isEqualTo(10);
        jsonPathAssert.jsonPathAsString("$.foo[0]").isEqualTo("hello");
        jsonPathAssert.jsonPathAsString("$.foo[1]").isEqualTo("world");
    }
}