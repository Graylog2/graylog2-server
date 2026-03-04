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
package org.graylog.storage.opensearch3.sniffer;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.DynamicTransport;
import org.graylog.storage.opensearch3.OfficialOpensearchClientProvider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.plugin.periodical.Periodical;
import org.opensearch.client.transport.OpenSearchTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeDiscoveryPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(NodeDiscoveryPeriodical.class);

    private final SnifferAggregator snifferAggregator;
    private final OfficialOpensearchClientProvider clientProvider;
    private final DynamicTransport dynamicTransport;
    private final ElasticsearchClientConfiguration configuration;

    private volatile Set<URI> lastKnownNodes = Set.of();

    @Inject
    public NodeDiscoveryPeriodical(SnifferAggregator snifferAggregator,
                                   OfficialOpensearchClientProvider clientProvider,
                                   DynamicTransport dynamicTransport,
                                   ElasticsearchClientConfiguration configuration) {
        this.snifferAggregator = snifferAggregator;
        this.clientProvider = clientProvider;
        this.dynamicTransport = dynamicTransport;
        this.configuration = configuration;
    }

    @Override
    public void doRun() {
        final List<DiscoveredNode> discovered = snifferAggregator.sniff();

        if (discovered.isEmpty()) {
            LOG.warn("Node discovery returned no nodes. Keeping current transport.");
            return;
        }

        final Set<URI> currentNodes = discovered.stream()
                .map(DiscoveredNode::toURI)
                .collect(Collectors.toSet());

        if (currentNodes.equals(lastKnownNodes)) {
            LOG.debug("Discovered nodes unchanged, no transport swap needed.");
            return;
        }

        LOG.info("Node list changed from {} to {}. Swapping transport.", lastKnownNodes, currentNodes);
        final OpenSearchTransport newTransport = clientProvider.buildTransportForNodes(discovered);
        dynamicTransport.swap(newTransport);
        lastKnownNodes = currentNodes;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return configuration.discoveryEnabled() || configuration.isNodeActivityLogger();
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return (int) configuration.discoveryFrequency().toSeconds();
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
