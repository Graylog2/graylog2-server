package org.graylog.storage.opensearch2;

import com.github.joschi.jadconfig.util.Duration;
import org.graylog.shaded.opensearch2.org.opensearch.client.Node;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.OpenSearchNodesSniffer;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.Sniffer;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class SnifferWrapper implements org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer {
    private final List<NodesSniffer> sniffers = new CopyOnWriteArrayList();
    private final RestClient restClient;
    private final  long sniffRequestTimeoutMillis;
    private final  Duration discoveryFrequency;
    private final OpenSearchNodesSniffer.Scheme scheme;
    private org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer nodesSniffer;

    private SnifferWrapper(RestClient restClient, long sniffRequestTimeoutMillis, Duration discoveryFrequency, OpenSearchNodesSniffer.Scheme scheme) {
        this.restClient = restClient;
        this.sniffRequestTimeoutMillis = sniffRequestTimeoutMillis;
        this.discoveryFrequency = discoveryFrequency;
        this.scheme = scheme;
    }

    @Override
    public List<Node> sniff() throws IOException {
        List<Node> nodes = this.nodesSniffer.sniff();
        for (NodesSniffer sniffer : sniffers) {
            nodes = sniffer.sniff(nodes);
        }
        return nodes;
    }

    public static SnifferWrapper create(RestClient restClient, long sniffRequestTimeoutMillis, Duration discoveryFrequency, OpenSearchNodesSniffer.Scheme scheme) {
        return new SnifferWrapper(restClient, sniffRequestTimeoutMillis, discoveryFrequency, scheme);
    }

    public Optional<Sniffer> build() {
        if(sniffers.isEmpty()) {
            return Optional.empty();
        }

        this.nodesSniffer = new OpenSearchNodesSniffer(restClient, sniffRequestTimeoutMillis, scheme);
        return Optional.of(Sniffer.builder(restClient)
                .setSniffIntervalMillis(Math.toIntExact(discoveryFrequency.toMilliseconds()))
                .setNodesSniffer(this)
                .build());
    }

    public void add(NodesSniffer sniffer) {
        this.sniffers.add(sniffer);
    }
}
