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
package org.graylog2;

import com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch;
import io.searchbox.client.JestClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.graylog2.indexer.counts.JestClientRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import javax.inject.Inject;
import java.io.IOException;
import java.net.ServerSocket;

import static com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;

public abstract class AbstractESTest {
    private static final Integer ES_HTTP_PORT = getFreePort();

    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule()
        .settings(Settings.builder().put("http.port", ES_HTTP_PORT).build())
        .build();

    @Rule
    public ElasticsearchRule elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();

    @Rule
    public JestClientRule jestClientRule = JestClientRule.forEsHttpPort(ES_HTTP_PORT);

    @Inject
    private Client client;

    private JestClient jestClient;

    @Before
    public void setUp() throws Exception {
        this.jestClient = jestClientRule.getJestClient();
    }

    protected Client client() {
        return client;
    }

    protected JestClient jestClient() {
        return jestClient;
    }

    private static Integer getFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Unable to find free http port for embedded elasticsearch, aborting test: ", e);
        }
    }
}
