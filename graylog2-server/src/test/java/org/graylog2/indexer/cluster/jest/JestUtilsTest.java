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
package org.graylog2.indexer.cluster.jest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
        when(resultMock.getJsonObject()).thenReturn(new JsonObject());
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

        final JsonObject rootCauseStub = new JsonObject();
        rootCauseStub.addProperty("reason", "foobar");
        final JsonArray rootCausesStub = new JsonArray();
        rootCausesStub.add(rootCauseStub);
        final JsonObject errorStub = new JsonObject();
        errorStub.add("root_cause", rootCausesStub);
        final JsonObject responseStub = new JsonObject();
        responseStub.add("error", errorStub);

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
        } catch (Exception e) {
            fail("Expected ElasticsearchException to be thrown");
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

        final JsonObject responseStub = new JsonObject();
        responseStub.addProperty("Message", "Authorization header requires 'Credential' parameter.");

        when(resultMock.getJsonObject()).thenReturn(responseStub);

        when(clientMock.execute(request)).thenReturn(resultMock);

        try {
            JestUtils.execute(clientMock, request, () -> "BOOM");
        } catch (ElasticsearchException e) {
            assertThat(e)
                .hasMessageStartingWith("BOOM")
                .hasMessageEndingWith("{\"Message\":\"Authorization header requires 'Credential' parameter.\"}")
                .hasNoSuppressedExceptions();
            assertThat(e.getErrorDetails()).containsExactly("{\"Message\":\"Authorization header requires 'Credential' parameter.\"}");
        } catch (Exception e) {
            fail("Expected QueryParsingException to be thrown");
        }
    }

    @Test
    public void executeWithQueryParsingException() throws Exception {
        final Ping request = new Ping.Builder().build();

        final JestResult resultMock = mock(JestResult.class);
        when(resultMock.isSucceeded()).thenReturn(false);

        final JsonObject rootCauseStub = new JsonObject();
        rootCauseStub.addProperty("type", "query_parsing_exception");
        rootCauseStub.addProperty("reason", "foobar");
        rootCauseStub.addProperty("line", 23);
        rootCauseStub.addProperty("col", 42);
        rootCauseStub.addProperty("index", "my_index");
        final JsonArray rootCausesStub = new JsonArray();
        rootCausesStub.add(rootCauseStub);
        final JsonObject errorStub = new JsonObject();
        errorStub.add("root_cause", rootCausesStub);
        final JsonObject responseStub = new JsonObject();
        responseStub.add("error", errorStub);

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
        } catch (Exception e) {
            fail("Expected QueryParsingException to be thrown");
        }
    }
}
