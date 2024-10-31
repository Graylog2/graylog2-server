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
package org.graylog.plugins.views.search.rest.export.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.bson.Document;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.export.ExportTabularResultResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.graylog.plugins.views.search.searchtypes.export.ExportTabularResultResponse.DataRow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportTabularResultResponseTest {

    @Test
    void testCreationFromPivotResult() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final byte[] bytes = Resources.toByteArray(Resources.getResource("org/graylog/plugins/views/search/rest/export/response/sample_pivot_result.json"));
        final PivotResult pivotResult = objectMapper.readValue(bytes, PivotResult.class);
        final ExportTabularResultResponse response = ExportTabularResultResponse.fromPivotResult(pivotResult);

        final ExportTabularResultResponse expectedResponse = new ExportTabularResultResponse(
                List.of("", "[count()]", "[max(http_response_code)]", "[DELETE, count()]", "[DELETE, max(http_response_code)]", "[GET, count()]", "[GET, max(http_response_code)]", "[POST, count()]", "[POST, max(http_response_code)]", "[PUT, count()]", "[PUT, max(http_response_code)]"),
                List.of(
                        new DataRow(List.of("index", 1507337, 504, 75322, 504, 1296526, 504, 75163, 504, 60326, 504)),
                        new DataRow(List.of("show", 444038, 504, 22229, 504, 381846, 504, 22271, 504, 17692, 504)),
                        new DataRow(List.of("login", 377715, 504, 18773, 204, 325040, 200, 18761, 504, 15141, 504)),
                        new DataRow(List.of("edit", 68699, 504, 3540, 504, 58912, 504, 3488, 504, 2759, 504))
                )
        );

        assertEquals(expectedResponse, response);
    }


    @Test
    void testCreationFromDocumentList() {
        List<Document> documents = List.of(
                new Document(Map.of("name", "Juan", "age", 42)),
                new Document(Map.of("name", "Enrique", "age", 13))
        );

        final ExportTabularResultResponse response = ExportTabularResultResponse.fromDocumentList(documents, List.of("name", "age", "non-existing"));

        final ExportTabularResultResponse expectedResponse = new ExportTabularResultResponse(
                List.of("name", "age", "non-existing"),
                List.of(
                        new DataRow(Arrays.asList("Juan", 42, null)),
                        new DataRow(Arrays.asList("Enrique", 13, null))
                )
        );

        assertEquals(expectedResponse, response);

    }

    @Test
    void testCreationFromMessageList() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final byte[] bytes = Resources.toByteArray(Resources.getResource("org/graylog/plugins/views/search/rest/export/response/sample_message_list_result.json"));
        final MessageList.Result messageListResult = objectMapper.readValue(bytes, MessageList.Result.class);
        final ExportTabularResultResponse response = ExportTabularResultResponse.fromMessageListResult(messageListResult);

        final ExportTabularResultResponse expectedResponse = new ExportTabularResultResponse(
                List.of("sequence_nr","ingest_time","gl2_receive_timestamp","took_ms","source"),
                List.of(
                        new DataRow(List.of(277, "2024-05-06T12:35:52.284Z", "2024-05-06 12:35:52.284", 48, "example.org")),
                        new DataRow(List.of(278, "2024-05-06T12:35:52.284Z", "2024-05-06 12:35:52.284", 49, "example.org"))
                )
        );

        assertEquals(expectedResponse, response);
    }
}
