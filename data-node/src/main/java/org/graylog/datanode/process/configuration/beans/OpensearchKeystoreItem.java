package org.graylog.datanode.process.configuration.beans;

import org.graylog.datanode.opensearch.cli.OpensearchKeystoreCli;

public interface OpensearchKeystoreItem {
    String key();
    void persist(OpensearchKeystoreCli cli);
}
