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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import io.searchbox.client.JestClient;
import io.searchbox.client.http.JestHttpClient;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class JestClientProviderTest {
    @Test
    public void getReturnsJestHttpClient() {
        final JestClientProvider provider = new JestClientProvider(
                Collections.singletonList(URI.create("http://127.0.0.1:9200")),
                Duration.seconds(5L),
                Duration.seconds(5L),
                Duration.seconds(5L),
                8,
                2,
                2,
                false,
                null,
                Duration.seconds(5L),
                "http",
                false,
                new ObjectMapper()
        );

        final JestClient jestClient = provider.get();
        assertThat(jestClient).isInstanceOf(JestHttpClient.class);
    }

    @Test
    public void preemptiveAuthWithoutTrailingSlash() {
        final JestClientProvider provider = new JestClientProvider(
                Collections.singletonList(URI.create("http://elastic:changeme@127.0.0.1:9200")),
                Duration.seconds(5L),
                Duration.seconds(5L),
                Duration.seconds(5L),
                8,
                2,
                2,
                false,
                null,
                Duration.seconds(5L),
                "http",
                false,
                new ObjectMapper()
        );

        final JestClient jestClient = provider.get();
        assertThat(jestClient).isInstanceOf(JestHttpClient.class);
    }

    @Test
    public void preemptiveAuthWithTrailingSlash() {
        final JestClientProvider provider = new JestClientProvider(
                Collections.singletonList(URI.create("http://elastic:changeme@127.0.0.1:9200/")),
                Duration.seconds(5L),
                Duration.seconds(5L),
                Duration.seconds(5L),
                8,
                2,
                2,
                false,
                null,
                Duration.seconds(5L),
                "http",
                false,
                new ObjectMapper()
        );

        final JestClient jestClient = provider.get();
        assertThat(jestClient).isInstanceOf(JestHttpClient.class);
    }
}
