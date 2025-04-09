package org.graylog.datanode.process.configuration.beans;

import org.graylog.datanode.opensearch.cli.OpensearchKeystoreCli;

import java.nio.file.Path;

public class OpensearchKeystoreFileItem implements OpensearchKeystoreItem {

    private final String key;
    private final Path file;

    public OpensearchKeystoreFileItem(final String key, final Path file) {
        this.key = key;
        this.file = file;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public void persist(OpensearchKeystoreCli cli) {
        cli.addFile(key, file);
    }
}
