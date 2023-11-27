package org.graylog2.indexer.datanode;

import java.util.List;

public interface RemoteReindexingMigrationAdapter {
    enum Status {
        STARTING, RUNNING, ERROR, FINISHED
    }

    void start(String host, String username, String password, List<String> indices);

    Status status();
}
