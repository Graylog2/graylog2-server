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
package org.graylog2.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.revinate.assertj.json.JsonPathAssert;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.util.function.Consumer;

public class AssertJsonPath {
    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private static final Configuration configuration = Configuration.builder()
            .mappingProvider(new JacksonMappingProvider(objectMapper))
            .jsonProvider(new JacksonJsonProvider(objectMapper))
            .build();


    public static void assertJsonPath(Object obj, Consumer<JsonPathAssert> consumer) {
        assertJsonPath(obj.toString(), consumer);
    }

    public static void assertJsonPath(String json, Consumer<JsonPathAssert> consumer) {
        final DocumentContext context = JsonPath.parse(json, configuration);
        final JsonPathAssert jsonPathAssert = JsonPathAssert.assertThat(context);

        consumer.accept(jsonPathAssert);
    }
}
