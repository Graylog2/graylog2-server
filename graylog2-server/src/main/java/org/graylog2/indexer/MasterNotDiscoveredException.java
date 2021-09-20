package org.graylog2.indexer;

public class MasterNotDiscoveredException extends ElasticsearchException {
    public MasterNotDiscoveredException() {
        super("Cluster has not elected a master.");
    }
}
