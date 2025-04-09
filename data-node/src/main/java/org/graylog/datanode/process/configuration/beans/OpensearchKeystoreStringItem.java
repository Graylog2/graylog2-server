package org.graylog.datanode.process.configuration.beans;

import org.graylog.datanode.opensearch.cli.OpensearchKeystoreCli;

public class OpensearchKeystoreStringItem implements OpensearchKeystoreItem {

    private final String key;
    private final String secret;

    public OpensearchKeystoreStringItem(final String key, final String secret) {
        this.key = key;
        this.secret = secret;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public void persist(OpensearchKeystoreCli cli) {
        cli.add(key, secret);
    }
}
