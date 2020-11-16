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
package org.graylog.storage.elasticsearch6.jest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.QueryParsingException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JestUtilsTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JestClient clientMock;

    @Test
    public void executeWithSuccessfulResponse() throws Exception {
        final Ping request = new Ping.Builder().build();
        final JestResult resultMock = mock(JestResult.class);
        when(resultMock.isSucceeded()).thenReturn(true);
        when(clientMock.execute(request)).thenReturn(resultMock);

        final JestResult result = JestUtils.execute(clientMock, request, () -> "BOOM");
        assertThat(result.isSucceeded()).isTrue();
    }

    @Test
    public void executeWithUnsuccessfulResponse() throws Exception {
        final Ping request = new Ping.Builder().build();
        final JestResult resultMock = mock(JestResult.class);
        when(resultMock.isSucceeded()).thenReturn(false);
        when(resultMock.getJsonObject()).thenReturn(objectMapper.createObjectNode());
        when(clientMock.execute(request)).thenReturn(resultMock);

        expectedException.expect(ElasticsearchException.class);
        expectedException.expectMessage("BOOM");

        JestUtils.execute(clientMock, request, () -> "BOOM");
    }

    @Test
    public void executeWithUnsuccessfulResponseAndErrorDetails() throws Exception {
        final Ping request = new Ping.Builder().build();

        final JestResult resultMock = mock(JestResult.class);
        when(resultMock.isSucceeded()).thenReturn(false);

        final ObjectNode rootCauseStub = objectMapper.createObjectNode();
        rootCauseStub.set("reason", new TextNode("foobar"));
        final ArrayNode rootCausesStub = objectMapper.createArrayNode();
        rootCausesStub.add(rootCauseStub);
        final ObjectNode errorStub = objectMapper.createObjectNode();
        errorStub.set("root_cause", rootCausesStub);
        final ObjectNode responseStub = objectMapper.createObjectNode();
        responseStub.set("error", errorStub);

        when(resultMock.getJsonObject()).thenReturn(responseStub);
        when(clientMock.execute(request)).thenReturn(resultMock);

        try {
            JestUtils.execute(clientMock, request, () -> "BOOM");
            fail("Expected ElasticsearchException to be thrown");
        } catch (ElasticsearchException e) {
            assertThat(e)
                    .hasMessageStartingWith("BOOM")
                    .hasMessageEndingWith("foobar")
                    .hasNoSuppressedExceptions();
            assertThat(e.getErrorDetails()).containsExactly("foobar");
        }
    }

    @Test
    public void executeWithIOException() throws Exception {
        final Ping request = new Ping.Builder().build();
        when(clientMock.execute(request)).thenThrow(IOException.class);

        expectedException.expect(ElasticsearchException.class);
        expectedException.expectMessage("BOOM");

        JestUtils.execute(clientMock, request, () -> "BOOM");
    }

    @Test
    public void executeFailsWithCustomMessage() throws Exception {
        final Ping request = new Ping.Builder().build();

        final JestResult resultMock = mock(JestResult.class);
        when(resultMock.isSucceeded()).thenReturn(false);

        final ObjectNode responseStub = objectMapper.createObjectNode();
        final ObjectNode errorStub = objectMapper.createObjectNode();
        responseStub.set("Message", new TextNode("Authorization header requires 'Credential' parameter."));
        errorStub.set("error", responseStub);

        when(resultMock.getJsonObject()).thenReturn(errorStub);

        when(clientMock.execute(request)).thenReturn(resultMock);

        try {
            JestUtils.execute(clientMock, request, () -> "BOOM");
            fail("Expected ElasticsearchException to be thrown");
        } catch (ElasticsearchException e) {
            assertThat(e)
                .hasMessageStartingWith("BOOM")
                .hasMessageEndingWith("{\"Message\":\"Authorization header requires 'Credential' parameter.\"}")
                .hasNoSuppressedExceptions();
            assertThat(e.getErrorDetails()).containsExactly("{\"Message\":\"Authorization header requires 'Credential' parameter.\"}");
        }
    }

    @Test
    public void executeWithQueryParsingException() throws Exception {
        final Ping request = new Ping.Builder().build();

        final JestResult resultMock = mock(JestResult.class);
        when(resultMock.isSucceeded()).thenReturn(false);

        final ObjectNode rootCauseStub = objectMapper.createObjectNode();
        rootCauseStub.set("type", new TextNode("query_parsing_exception"));
        rootCauseStub.set("reason", new TextNode("foobar"));
        rootCauseStub.set("line", new IntNode(23));
        rootCauseStub.set("col", new IntNode(42));
        rootCauseStub.set("index", new TextNode("my_index"));
        final ArrayNode rootCausesStub = objectMapper.createArrayNode();
        rootCausesStub.add(rootCauseStub);
        final ObjectNode errorStub = objectMapper.createObjectNode();
        errorStub.set("root_cause", rootCausesStub);
        final ObjectNode responseStub = objectMapper.createObjectNode();
        responseStub.set("error", errorStub);

        when(resultMock.getJsonObject()).thenReturn(responseStub);
        when(clientMock.execute(request)).thenReturn(resultMock);

        try {
            JestUtils.execute(clientMock, request, () -> "BOOM");
            fail("Expected QueryParsingException to be thrown");
        } catch (QueryParsingException e) {
            assertThat(e)
                    .hasMessageStartingWith("BOOM")
                    .hasMessageEndingWith("foobar")
                    .hasNoSuppressedExceptions();
            assertThat(e.getErrorDetails()).containsExactly("foobar");
            assertThat(e.getLine()).contains(23);
            assertThat(e.getColumn()).contains(42);
            assertThat(e.getIndex()).contains("my_index");
        }
    }
}
