package org.graylog.storage.opensearch2;

import org.graylog.shaded.opensearch2.org.opensearch.client.Node;

import java.io.IOException;
import java.util.List;

public interface NodesSniffer {
    List<Node> sniff(List<Node> nodes) throws IOException;
}
