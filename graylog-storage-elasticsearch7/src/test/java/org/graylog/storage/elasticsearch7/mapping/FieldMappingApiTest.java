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
package org.graylog.storage.elasticsearch7.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class FieldMappingApiTest {

    FieldMappingApi toTest;
    ElasticsearchClient client;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        client = mock(ElasticsearchClient.class);
        toTest = new FieldMappingApi(client);
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    void testAliasTypeIsProperlyResolved() throws Exception {
        String mappingResponse = """
                {
                  "graylog_42": {
                    "mappings": {
                      "properties": {
                        "action_alias": {
                          "type": "alias",
                          "path": "action"
                        },
                        "action": {
                          "type": "keyword"
                        }
                      }
                    }
                  }
                }
                """;
        doReturn(objectMapper.readTree(mappingResponse))
                .when(client)
                .executeRequest(eq(new Request("GET", "/graylog_42/_mapping")), anyString());

        final Map<String, FieldMappingApi.FieldMapping> expectedResult = Map.of(
                "action_alias", FieldMappingApi.FieldMapping.create("keyword", false),
                "action", FieldMappingApi.FieldMapping.create("keyword", false)
        );
        final Map<String, FieldMappingApi.FieldMapping> result = toTest.fieldTypes("graylog_42");
        assertEquals(expectedResult, result);
    }


}
