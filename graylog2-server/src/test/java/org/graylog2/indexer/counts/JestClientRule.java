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
        final JestClientProvider jestClientProvider = new JestClientProvider(ImmutableList.of(esUri), 10000, 60000, Duration.of(60, ChronoUnit.SECONDS));
        return jestClientProvider.get();
    }

    public static JestClientRule forEsHttpPort(Integer esHttpPort) {
        return new JestClientRule(esHttpPort);
    }
}
