package org.graylog.storage.opensearch2;

import org.junit.Test;

import java.util.List;

public class RemoteReindexingMigrationAdapterOS2Test {
    @Test
    public void testMigrate() {
        RemoteReindexingMigrationAdapterOS2 adapter = new RemoteReindexingMigrationAdapterOS2(null);
        adapter.start("http://localhost:9201", "admin", "admin", List.of("index1"));
    }
}
