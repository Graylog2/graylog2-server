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

import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import org.graylog2.bindings.providers.JestClientProvider;
import org.junit.rules.ExternalResource;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class JestClientRule extends ExternalResource {
    private final Integer esHttpPort;

    private JestClientRule(Integer esHttpPort) {
        this.esHttpPort = esHttpPort;
    }

    public JestClient getJestClient() {
        final URI esUri = URI.create("http://localhost:" + esHttpPort);
        final JestClientProvider jestClientProvider = new JestClientProvider(
            ImmutableList.of(esUri),
            Duration.ofSeconds(10),
            Duration.ofSeconds(60),
            Duration.of(60, ChronoUnit.SECONDS),
            20,
            2
        );
        return jestClientProvider.get();
    }

    public static JestClientRule forEsHttpPort(Integer esHttpPort) {
        return new JestClientRule(esHttpPort);
    }
}
