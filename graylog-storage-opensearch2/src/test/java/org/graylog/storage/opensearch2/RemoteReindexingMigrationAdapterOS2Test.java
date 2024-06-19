package org.graylog.storage.opensearch2;

import org.assertj.core.api.Assertions;
import org.graylog2.indexer.datanode.RemoteReindexRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

class RemoteReindexingMigrationAdapterOS2Test {

    @Test
    void parseAllowlistWithHttpProtocol() {
        final RemoteReindexRequest req = new RemoteReindexRequest("http://example.com:9200", URI.create("http://example.com:9200"), "", "", Collections.emptyList(), 4);
        final List<String> allowList = RemoteReindexingMigrationAdapterOS2.parseAllowlist(req);
        Assertions.assertThat(allowList)
                .containsExactly("example.com:9200");
    }


    @Test
    void parseAllowlistWithHttpsProtocol() {
        final RemoteReindexRequest req = new RemoteReindexRequest("https://example.com:9200", URI.create("https://example.com:9200"), "", "", Collections.emptyList(), 4);
        final List<String> allowList = RemoteReindexingMigrationAdapterOS2.parseAllowlist(req);
        Assertions.assertThat(allowList)
                .containsExactly("example.com:9200");
    }

    @Test
    void parseAllowlistWithoutProtocol() {
        final RemoteReindexRequest req = new RemoteReindexRequest("example.com:9200", URI.create("http://example.com:9200"), "", "", Collections.emptyList(), 4);
        final List<String> allowList = RemoteReindexingMigrationAdapterOS2.parseAllowlist(req);
        Assertions.assertThat(allowList)
                .containsExactly("example.com:9200");
    }
}
