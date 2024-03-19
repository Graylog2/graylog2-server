/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.opensearch2.testing;

import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.datanode.DatanodeDevContainerInstanceProvider;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.List;
import java.util.Map;

public class DatanodeDevInstance extends OpenSearchInstance {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeDevInstance.class);
    public static final SearchServer DATANODE_VERSION = SearchServer.DATANODE_DEV;
    private final String mongoDBUri;
    private final String passwordSecret;
    private final String rootPasswordSha2;

    public DatanodeDevInstance(final SearchVersion version,
                               final String hostname,
                               final Network network,
                               final String mongoDBUri,
                               final String passwordSecret,
                               final String rootPasswordSha2,
                               final String heapSize,
                               final List<String> featureFlags,
                               final Map<String, String> env) {
        super(version, hostname, network, heapSize, featureFlags, env);
        this.mongoDBUri = mongoDBUri;
        this.passwordSecret = passwordSecret;
        this.rootPasswordSha2 = rootPasswordSha2;
    }

    @Override
    public DatanodeDevInstance init() {
        super.init();
        return this;
    }

    @Override
    protected String imageName() {
        return "creating image locally by provider";
    }

    @Override
    public SearchServer searchServer() {
        return DATANODE_VERSION;
    }

    @Override
    public GenericContainer<?> buildContainer(String image, Network network) {
        var builder = DatanodeDevContainerInstanceProvider.getBuilderFor(this.version()).orElseThrow(() -> new UnsupportedOperationException("Can not build container for Search version " + this.version() + " - not supported."));

        builder.nodeName(hostname).passwordSecret(passwordSecret).rootPasswordSha2(rootPasswordSha2).network(network).mongoDbUri(mongoDBUri).restPort(8999).openSearchHttpPort(9200).openSearchTransportPort(9300);

        return builder.build();
    }
}
