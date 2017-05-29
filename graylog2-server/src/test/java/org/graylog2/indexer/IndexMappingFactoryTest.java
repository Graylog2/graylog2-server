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
package org.graylog2.indexer;

import com.github.zafarkhaja.semver.ParseException;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexMappingFactoryTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JestClient jestClient;
    private IndexMappingFactory indexMappingFactory;

    @Before
    public void setUp() throws Exception {
        indexMappingFactory = new IndexMappingFactory(jestClient);
    }

    @Test
    public void createIndexMappingFailsIfElasticsearchIsUnavailable() throws Exception {
        when(jestClient.execute(any(Ping.class))).thenThrow(IOException.class);

        assertThatThrownBy(() -> indexMappingFactory.createIndexMapping())
                .isInstanceOf(ElasticsearchException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void createIndexMappingFailsIfElasticsearchResponseFailed() throws Exception {
        final JestResult failedResult = mock(JestResult.class);
        when(failedResult.isSucceeded()).thenReturn(false);
        when(failedResult.getJsonObject()).thenReturn(new JsonObject());
        when(jestClient.execute(any(Ping.class))).thenReturn(failedResult);
        assertThatThrownBy(() -> indexMappingFactory.createIndexMapping())
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unable to retrieve Elasticsearch version")
                .hasNoCause();
    }

    @Test
    public void createIndexMappingFailsIfElasticsearchVersionIsTooLow() throws Exception {
        final JestResult jestResult = mock(JestResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getJsonObject()).thenReturn(buildVersionJsonObject("1.7.3"));
        when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

        assertThatThrownBy(() -> indexMappingFactory.createIndexMapping())
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Elasticsearch version: 1.7.3")
                .hasNoCause();
    }

    @Test
    public void createIndexMappingFailsIfElasticsearch2VersionIsTooLow() throws Exception {
        final JestResult jestResult = mock(JestResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getJsonObject()).thenReturn(buildVersionJsonObject("2.0.0"));
        when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

        assertThatThrownBy(() -> indexMappingFactory.createIndexMapping())
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Elasticsearch version: 2.0.0")
                .hasNoCause();
    }

    @Test
    public void createIndexMappingFailsIfElasticsearchVersionIsTooHigh() throws Exception {
        final JestResult jestResult = mock(JestResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getJsonObject()).thenReturn(buildVersionJsonObject("6.0.0"));
        when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

        assertThatThrownBy(() -> indexMappingFactory.createIndexMapping())
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Elasticsearch version: 6.0.0")
                .hasNoCause();
    }

    @Test
    public void createIndexMappingFailsIfElasticsearchVersionIsInvalid() throws Exception {
        final JestResult jestResult = mock(JestResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getJsonObject()).thenReturn(buildVersionJsonObject("Foobar"));
        when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

        assertThatThrownBy(() -> indexMappingFactory.createIndexMapping())
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unable to parse Elasticsearch version: Foobar")
                .hasCauseInstanceOf(ParseException.class);
    }

    private static JsonObject buildVersionJsonObject(String foobar) {
        final JsonObject versionObject = new JsonObject();
        versionObject.addProperty("number", foobar);
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("version", versionObject);
        return jsonObject;
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTest {
        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"2.1.0", IndexMapping2.class},
                    {"2.2.0", IndexMapping2.class},
                    {"2.3.0", IndexMapping2.class},
                    {"2.4.0", IndexMapping2.class},
                    {"2.4.5", IndexMapping2.class},
                    {"5.0.0", IndexMapping5.class},
                    {"5.1.0", IndexMapping5.class},
                    {"5.2.0", IndexMapping5.class},
                    {"5.3.0", IndexMapping5.class},
                    {"5.4.0", IndexMapping5.class},
            });
        }

        @Rule
        public final MockitoRule mockitoRule = MockitoJUnit.rule();

        private final String version;
        private final Class<? extends IndexMapping> expectedMapping;

        @Mock
        private JestClient jestClient;
        private IndexMappingFactory indexMappingFactory;


        public ParameterizedTest(String version, Class<? extends IndexMapping> expectedMapping) {
            this.version = version;
            this.expectedMapping = expectedMapping;
        }

        @Before
        public void setUp() throws Exception {
            indexMappingFactory = new IndexMappingFactory(jestClient);
        }

        @Test
        public void test() throws Exception {
            final JestResult jestResult = mock(JestResult.class);
            when(jestResult.isSucceeded()).thenReturn(true);
            final JsonObject jsonObject = buildVersionJsonObject(version);
            when(jestResult.getJsonObject()).thenReturn(jsonObject);
            when(jestClient.execute(any(Ping.class))).thenReturn(jestResult);

            assertThat(indexMappingFactory.createIndexMapping()).isInstanceOf(expectedMapping);
        }
    }
}