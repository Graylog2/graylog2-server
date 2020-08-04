package org.graylog.storage.elasticsearch7;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Node;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.NodesSniffer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class FilteredElasticsearchNodesSniffer implements NodesSniffer {
    private final NodesSniffer nodesSniffer;
    private final String attribute;
    private final String value;

    static FilteredElasticsearchNodesSniffer create(RestClient restClient, long sniffRequestTimeoutMillis, ElasticsearchNodesSniffer.Scheme scheme, String filter) {
        final String attribute;
        final String value;
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
        final NodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(restClient, sniffRequestTimeoutMillis, scheme);

        return new FilteredElasticsearchNodesSniffer(nodesSniffer, attribute, value);
    }

    @VisibleForTesting
    FilteredElasticsearchNodesSniffer(NodesSniffer nodesSniffer, String attribute, String value) {
        this.nodesSniffer = nodesSniffer;
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    public List<Node> sniff() throws IOException {
        final List<Node> nodes = this.nodesSniffer.sniff();

        if (attribute == null || value == null) {
            return nodes;
        }

        return nodes.stream()
                .filter(node -> nodeMatchesFilter(node, attribute, value))
                .collect(Collectors.toList());
    }

    private boolean nodeMatchesFilter(Node node, String attribute, String value) {
        return node.getAttributes()
                .getOrDefault(attribute, Collections.emptyList())
                .contains(value);
    }
}
