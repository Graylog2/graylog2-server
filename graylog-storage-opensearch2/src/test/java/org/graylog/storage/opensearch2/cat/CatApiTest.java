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
package org.graylog.storage.opensearch2.cat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatApiTest {

    private final static String SAMPLE_CAT_NODES_RESPONSE = "[" +
            "{" +
            "\"id\":\"nodeWithCorrectInfo\"," +
            "\"name\":\"nodeWithCorrectInfo\"," +
            "\"role\":\"dimr\"," +
            "\"ip\":\"182.88.0.2\"," +
            "\"fileDescriptorMax\":\"1048576\"," +
            "\"diskUsed\":\"45gb\"," +
            "\"diskTotal\":\"411.5gb\"," +
            "\"diskUsedPercent\":\"10.95\"" +
            "}," +
            "{" +
            "\"id\":\"nodeWithMissingDiskStatistics\"," +
            "\"name\":\"nodeWithMissingDiskStatistics\"," +
            "\"role\":\"dimr\"," +
            "\"ip\":\"182.88.0.1\"" +
            "}" +
            "]";

    @Test
    void testNodesMethodParsesAndReturnsEvenNodesThatMissDiskUsageInfo() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final OpenSearchClient client = mock(OpenSearchClient.class);
        final CatApi toTest = new CatApi(objectMapper, client);

        when(client.execute(any(), anyString()))
                .thenReturn(objectMapper.readValue(SAMPLE_CAT_NODES_RESPONSE, new TypeReference<List<NodeResponse>>() {}));

        final List<NodeResponse> nodes = toTest.nodes();

        assertThat(nodes)
                .hasSize(2)
                .contains(NodeResponse.create("nodeWithCorrectInfo",
                        "nodeWithCorrectInfo",
                        "dimr",
                        null,
                        "182.88.0.2",
                        "45gb",
                        "411.5gb",
                        10.95d,
                        1048576L))
                .contains(NodeResponse.create("nodeWithMissingDiskStatistics",
                        "nodeWithMissingDiskStatistics",
                        "dimr",
                        null,
                        "182.88.0.1",
                        null,
                        null,
                        null,
                        null));
    }
}
