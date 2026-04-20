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
package org.graylog.storage.opensearch3.mapping;

import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.testing.client.mock.ServerlessOpenSearchClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldMappingApiTest {

    @Test
    void testReturnsEmptyMapOnNoMappings() {
        String mappingResponse = """
                {
                  "graylog_13": {
                    "mappings": {
                      "properties": {
                      }
                    }
                  }
                }
                """;

        final OfficialOpensearchClient opensearchClient = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/graylog_13/_mapping", mappingResponse)
                .build();

        final FieldMappingApi api = new FieldMappingApi(opensearchClient);

        final Map<String, FieldMappingApi.FieldMapping> result = api.fieldTypes("graylog_13");
        assertEquals(Map.of(), result);
    }

    @Test
    void testParsesMappingsCorrectly() {
        String mappingResponse = """
                {
                  "graylog_42": {
                    "mappings": {
                      "properties": {
                        "action": {
                          "type": "keyword"
                        },
                        "text": {
                           "type": "text",
                           "analyzer": "analyzer_keyword",
                           "fielddata": true
                        },
                        "date": {
                          "type": "date"
                        },
                        "number": {
                          "type": "long",
                          "fielddata": "false"
                        }
                      }
                    }
                  }
                }
                """;

        final OfficialOpensearchClient opensearchClient = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/graylog_42/_mapping", mappingResponse)
                .build();

        final FieldMappingApi api = new FieldMappingApi(opensearchClient);

        final Map<String, FieldMappingApi.FieldMapping> expectedResult = Map.of("text", new FieldMappingApi.FieldMapping("text", true), "action", new FieldMappingApi.FieldMapping("keyword", false), "date", new FieldMappingApi.FieldMapping("date", false), "number", new FieldMappingApi.FieldMapping("long", false));
        final Map<String, FieldMappingApi.FieldMapping> result = api.fieldTypes("graylog_42");
        assertEquals(expectedResult, result);
    }

    @Test
    void testAliasTypeIsProperlyResolved() {
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
        final OfficialOpensearchClient opensearchClient = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/graylog_42/_mapping", mappingResponse)
                .build();

        final FieldMappingApi api = new FieldMappingApi(opensearchClient);

        final Map<String, FieldMappingApi.FieldMapping> expectedResult = Map.of("action_alias", new FieldMappingApi.FieldMapping("keyword", false), "action", new FieldMappingApi.FieldMapping("keyword", false));
        final Map<String, FieldMappingApi.FieldMapping> result = api.fieldTypes("graylog_42");
        assertEquals(expectedResult, result);
    }


}
