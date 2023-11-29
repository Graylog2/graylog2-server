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
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class IndexerDiscoveryProvider implements Provider<List<URI>> {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerDiscoveryProvider.class);

    public static final URI DEFAULT_INDEXER_HOST = URI.create("http://127.0.0.1:9200");

    private final List<URI> hosts;
    private final PreflightConfigService preflightConfigService;
    private final NodeService<DataNodeDto> nodeService;

    private final Supplier<List<URI>> resultsCachingSupplier;

    @Inject
    public IndexerDiscoveryProvider(
            @Named("elasticsearch_hosts") List<URI> hosts,
            PreflightConfigService preflightConfigService,
            NodeService<DataNodeDto> nodeService) {
        this.hosts = hosts;
        this.preflightConfigService = preflightConfigService;
        this.nodeService = nodeService;
        this.resultsCachingSupplier = Suppliers.memoize(this::doGet);
    }

    @Override
    public List<URI> get() {
        return resultsCachingSupplier.get();
    }

    private List<URI> doGet() {

        // configured hosts, just use these and don't try any detection
        if (hosts != null && !hosts.isEmpty()) {
            return hosts;
        }

        final PreflightConfigResult preflightResult = preflightConfigService.getPreflightConfigResult();

        // if preflight is finished, we assume that there will be some datanode registered via node-service.
        if (preflightResult == PreflightConfigResult.FINISHED) {
            final List<URI> discovered = discover();
            if (!discovered.isEmpty()) {
                return discovered;
            } else {
                // TODO: we could wait here(or in the preflight check) for a datanode to register.
                throw new IllegalStateException("No Datanode available, terminating.");
            }
        }

        // if there are no configured hosts and the preflight never has run or was skipped, we should fallback
        // to our old default localhost:9200 to preserve backwards compatibility.
        LOG.info("No indexer hosts configured, using fallback {}", DEFAULT_INDEXER_HOST);
        return Collections.singletonList(DEFAULT_INDEXER_HOST);

    }

    private List<URI> discover() {
        return nodeService.allActive().values().stream()
                .map(Node::getTransportAddress)
                .map(URI::create)
                .collect(Collectors.toList());
    }
}
