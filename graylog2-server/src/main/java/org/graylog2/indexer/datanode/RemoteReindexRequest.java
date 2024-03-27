package org.graylog2.indexer.datanode;

import java.net.URI;
import java.util.List;

public record RemoteReindexRequest(URI uri, String username, String password, List<String> indices, int threadsCount) {
}
