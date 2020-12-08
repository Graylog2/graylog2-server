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
package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeAdapterES6Test {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JestClient jestClient;

    private NodeAdapter nodeAdapter;

    @Before
    public void setUp() throws Exception {
        nodeAdapter = new NodeAdapterES6(jestClient);
    }

    @Test
    public void retrievingVersionFailsIfElasticsearchIsUnavailable() throws Exception {
        when(jestClient.execute(any(Ping.class))).thenThrow(IOException.class);

        assertThatThrownBy(() -> nodeAdapter.version())
            .isInstanceOf(ElasticsearchException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void retrievingVersionFailsIfElasticsearchResponseFailed() throws Exception {
        final JestResult failedResult = mock(JestResult.class);
        when(failedResult.isSucceeded()).thenReturn(false);
        when(failedResult.getJsonObject()).thenReturn(objectMapper.createObjectNode());
        when(jestClient.execute(any(Ping.class))).thenReturn(failedResult);

        assertThatThrownBy(() -> nodeAdapter.version())
            .isInstanceOf(ElasticsearchException.class)
            .hasMessageStartingWith("Unable to retrieve Elasticsearch version")
            .hasNoCause();
    }

    @Test
    public void retrievingVersionSucceedsIfElasticsearchVersionIsValid() throws Exception {
        final JestResult jestResult = mock(JestResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getJsonObject()).thenReturn(buildVersionJsonObject("5.4.0"));
        when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

        final Optional<String> elasticsearchVersion = nodeAdapter.version();

        assertThat(elasticsearchVersion).contains("5.4.0");
    }

    private JsonNode buildVersionJsonObject(String foobar) {
        final ObjectNode versionObject = objectMapper.createObjectNode();
        versionObject.set("number", new TextNode(foobar));
        final ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.set("version", versionObject);
        return jsonObject;
    }
}
