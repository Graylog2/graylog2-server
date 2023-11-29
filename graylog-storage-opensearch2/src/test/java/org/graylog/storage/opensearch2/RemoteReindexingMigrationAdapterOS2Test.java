package org.graylog.storage.opensearch2;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class RemoteReindexingMigrationAdapterOS2Test {
    @Test
    public void testMigrate() throws URISyntaxException {
        RemoteReindexingMigrationAdapterOS2 adapter = new RemoteReindexingMigrationAdapterOS2(null, null, null, null, null);
        adapter.start(new URI("http://localhost:9201"), "admin", "admin", List.of("index1"));
    }
}
