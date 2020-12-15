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
package org.graylog2.rest.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog2.database.PaginatedList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThatThrownBy(() -> jsonPathAssert.jsonPathAsString("$.context")).isInstanceOf(PathNotFoundException.class);
    }

    @Test
    public void serializeWithContext() throws Exception {
        final ImmutableList<String> values = ImmutableList.of("hello", "world");
        final ImmutableMap<String, Object> context = ImmutableMap.of("context1", "wow");
        final PaginatedList<String> paginatedList = new PaginatedList<>(values, values.size(), 1, 10);
        final PaginatedResponse<String> response = PaginatedResponse.create("foo", paginatedList, context);

        final DocumentContext ctx = JsonPath.parse(objectMapper.writeValueAsString(response));
        final JsonPathAssert jsonPathAssert = JsonPathAssert.assertThat(ctx);

        jsonPathAssert.jsonPathAsInteger("$.total").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.count").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.page").isEqualTo(1);
        jsonPathAssert.jsonPathAsInteger("$.per_page").isEqualTo(10);
        jsonPathAssert.jsonPathAsString("$.foo[0]").isEqualTo("hello");
        jsonPathAssert.jsonPathAsString("$.foo[1]").isEqualTo("world");
        jsonPathAssert.jsonPathAsString("$.context.context1").isEqualTo("wow");
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
        assertThatThrownBy(() -> jsonPathAssert.jsonPathAsString("$.context")).isInstanceOf(PathNotFoundException.class);
    }

    @Test
    public void serializeWithQueryAndContext() throws Exception {
        final ImmutableList<String> values = ImmutableList.of("hello", "world");
        final ImmutableMap<String, Object> context = ImmutableMap.of("context1", "wow");
        final PaginatedList<String> paginatedList = new PaginatedList<>(values, values.size(), 1, 10);
        final PaginatedResponse<String> response = PaginatedResponse.create("foo", paginatedList, "query1", context);

        final DocumentContext ctx = JsonPath.parse(objectMapper.writeValueAsString(response));
        final JsonPathAssert jsonPathAssert = JsonPathAssert.assertThat(ctx);

        jsonPathAssert.jsonPathAsString("$.query").isEqualTo("query1");
        jsonPathAssert.jsonPathAsInteger("$.total").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.count").isEqualTo(2);
        jsonPathAssert.jsonPathAsInteger("$.page").isEqualTo(1);
        jsonPathAssert.jsonPathAsInteger("$.per_page").isEqualTo(10);
        jsonPathAssert.jsonPathAsString("$.foo[0]").isEqualTo("hello");
        jsonPathAssert.jsonPathAsString("$.foo[1]").isEqualTo("world");
        jsonPathAssert.jsonPathAsString("$.context.context1").isEqualTo("wow");
    }
}
