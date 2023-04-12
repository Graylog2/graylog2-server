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
package org.graylog2.configuration;

import com.google.common.base.Suppliers;
import com.google.inject.Provider;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class IndexerDiscoveryProvider implements Provider<List<URI>> {

    public static final URI DEFAULT_INDEXER_HOST = URI.create("http://127.0.0.1:9200");
    private final List<URI> hosts;
    private final NodeService nodeService;
    private final Supplier<List<URI>> supplier;

    @Inject
    public IndexerDiscoveryProvider(@Named("elasticsearch_hosts") List<URI> hosts, NodeService nodeService) {
        this.hosts = hosts;
        this.nodeService = nodeService;
        this.supplier = Suppliers.memoize(this::doGet);
    }

    @Override
    public List<URI> get() {
        return supplier.get();
    }

    private List<URI> doGet() {
        if (hosts != null && !hosts.isEmpty()) {
            return hosts;
        }

        final List<URI> discovered = discover();
        if (!discovered.isEmpty()) {
            return discovered;
        }
        return Collections.singletonList(DEFAULT_INDEXER_HOST);
    }

    private List<URI> discover() {
        return nodeService.allActive(Node.Type.DATANODE).values().stream()
                .map(Node::getTransportAddress)
                .map(URI::create)
                .collect(Collectors.toList());
    }
}
