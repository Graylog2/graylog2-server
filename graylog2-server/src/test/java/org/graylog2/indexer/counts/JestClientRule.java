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
package org.graylog2.indexer.counts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import io.searchbox.indices.Refresh;
import org.graylog2.bindings.providers.JestClientProvider;
import org.junit.rules.ExternalResource;

import java.net.URI;

public class JestClientRule extends ExternalResource {
    private final JestClientProvider jestClientProvider;
    private JestClient jestClient;

    private JestClientRule(Integer esHttpPort) {
        final URI esUri = URI.create("http://localhost:" + esHttpPort);
        this.jestClientProvider = new JestClientProvider(
            ImmutableList.of(esUri),
            Duration.seconds(10),
            Duration.seconds(60),
            Duration.seconds(60),
            20,
            2,
            false,
            null,
            Duration.seconds(30),
            false,
            new ObjectMapper()
        );
    }

    public JestClient getJestClient() {
        return jestClient;
    }

    public static JestClientRule forEsHttpPort(Integer esHttpPort) {
        return new JestClientRule(esHttpPort);
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        this.jestClient = jestClientProvider.get();
        this.jestClient.execute(new Refresh.Builder().build());
    }

    @Override
    protected void after() {
        super.after();
        this.jestClient.shutdownClient();
    }
}
