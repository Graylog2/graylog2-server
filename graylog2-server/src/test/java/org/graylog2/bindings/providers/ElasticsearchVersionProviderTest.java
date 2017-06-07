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
package org.graylog2.bindings.providers;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.graylog2.indexer.ElasticsearchException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchVersionProviderTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JestClient jestClient;

    private ElasticsearchVersionProvider elasticsearchVersionProvider;

    @Before
    public void setUp() throws Exception {
        elasticsearchVersionProvider = new ElasticsearchVersionProvider(jestClient);
    }

    @Test
    public void retrievingVersionFailsIfElasticsearchIsUnavailable() throws Exception {
        when(jestClient.execute(any(Ping.class))).thenThrow(IOException.class);

        assertThatThrownBy(() -> elasticsearchVersionProvider.get())
            .isInstanceOf(ElasticsearchException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void retrievingVersionFailsIfElasticsearchResponseFailed() throws Exception {
        final JestResult failedResult = mock(JestResult.class);
        when(failedResult.isSucceeded()).thenReturn(false);
        when(failedResult.getJsonObject()).thenReturn(new JsonObject());
        when(jestClient.execute(any(Ping.class))).thenReturn(failedResult);

        assertThatThrownBy(() -> elasticsearchVersionProvider.get())
            .isInstanceOf(ElasticsearchException.class)
            .hasMessageStartingWith("Unable to retrieve Elasticsearch version")
            .hasNoCause();
    }

    @Test
    public void retrievingVersionFailsIfElasticsearchVersionIsInvalid() throws Exception {
        final JestResult jestResult = mock(JestResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getJsonObject()).thenReturn(buildVersionJsonObject("Foobar"));
        when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

        assertThatThrownBy(() -> elasticsearchVersionProvider.get())
            .isInstanceOf(ElasticsearchException.class)
            .hasMessageStartingWith("Unable to parse Elasticsearch version: Foobar")
            .hasCauseInstanceOf(ParseException.class);
    }

    @Test
    public void retrievingVersionSucceedsIfElasticsearchVersionIsValid() throws Exception {
        final JestResult jestResult = mock(JestResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getJsonObject()).thenReturn(buildVersionJsonObject("5.4.0"));
        when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

        final Version elasticsearchVersion = elasticsearchVersionProvider.get();

        assertThat(elasticsearchVersion.toString()).isEqualTo("5.4.0");
    }

    private static JsonObject buildVersionJsonObject(String foobar) {
        final JsonObject versionObject = new JsonObject();
        versionObject.addProperty("number", foobar);
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("version", versionObject);
        return jsonObject;
    }
}
