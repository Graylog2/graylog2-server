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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class GetTaskResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    void testDeserializeCausedByObject() throws JsonProcessingException {
        final String body = """
                {
                  "error": {
                    "root_cause": [
                      {
                        "type": "index_not_found_exception",
                        "reason": "no such index [.tasks]",
                        "index": ".tasks",
                        "resource.id": ".tasks",
                        "resource.type": "index_expression",
                        "index_uuid": "_na_"
                      }
                    ],
                    "type": "resource_not_found_exception",
                    "reason": "task [l9jA-SSXRdm8rye9WK51dg:9575] belongs to the node [l9jA-SSXRdm8rye9WK51dg] which isn't part of the cluster and there is no record of the task",
                    "caused_by": {
                      "type": "resource_not_found_exception",
                      "reason": "task [l9jA-SSXRdm8rye9WK51dg:9575] isn't running and hasn't stored its results",
                      "caused_by": {
                        "type": "index_not_found_exception",
                        "reason": "no such index [.tasks]",
                        "index": ".tasks",
                        "resource.id": ".tasks",
                        "resource.type": "index_expression",
                        "index_uuid": "_na_"
                      }
                    }
                  },
                  "status": 404
                }
                """;
        final GetTaskResponse task = objectMapper.readValue(body, GetTaskResponse.class);
        Assertions.assertThat(task.error().type()).isEqualTo("resource_not_found_exception");
        Assertions.assertThat(task.error().reason()).contains("task [l9jA-SSXRdm8rye9WK51dg:9575] belongs to the node [l9jA-SSXRdm8rye9WK51dg]");
        Assertions.assertThat(task.error().causedBy()).isInstanceOf(Map.class).extracting(m -> (Map<String, Object>) m).satisfies(m -> {
            Assertions.assertThat(m.get("type")).isEqualTo("resource_not_found_exception");
            Assertions.assertThat(m.get("reason")).isEqualTo("task [l9jA-SSXRdm8rye9WK51dg:9575] isn't running and hasn't stored its results");
        });
    }
}
