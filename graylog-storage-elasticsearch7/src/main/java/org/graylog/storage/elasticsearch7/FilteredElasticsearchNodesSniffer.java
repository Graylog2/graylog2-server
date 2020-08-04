package org.graylog.storage.elasticsearch7;

import com.google.common.base.Strings;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Node;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.NodesSniffer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

class FilteredElasticsearchNodesSniffer implements NodesSniffer {
    private final ElasticsearchNodesSniffer nodesSniffer;
    private final String attribute;
    private final String value;

    public FilteredElasticsearchNodesSniffer(RestClient restClient, long sniffRequestTimeoutMillis, ElasticsearchNodesSniffer.Scheme scheme, String filter) {
        if (!Strings.isNullOrEmpty(filter)) {
            final String[] conditions = filter.split(":");
            if (conditions.length < 2) {
                throw new IllegalArgumentException("Invalid filter specified for ES node discovery: " + filter);
            }
            attribute = conditions[0].trim();
            value = conditions[1].trim();
        } else {
            attribute = null;
            value = null;
        }
        this.nodesSniffer = new ElasticsearchNodesSniffer(restClient, sniffRequestTimeoutMillis, scheme);
    }

    @Override
    public List<Node> sniff() throws IOException {
        final List<Node> nodes = this.nodesSniffer.sniff();

        if (attribute == null || value == null) {
            return nodes;
        }

        return nodes.stream()
                .filter(node -> node.getAttributes().get(attribute).contains(value))
                .collect(Collectors.toList());
    }
}
