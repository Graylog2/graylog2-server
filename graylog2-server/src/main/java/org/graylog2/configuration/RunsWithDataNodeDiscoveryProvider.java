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
import javax.inject.Provider;
import java.net.URI;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class RunsWithDataNodeDiscoveryProvider implements Provider<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(RunsWithDataNodeDiscoveryProvider.class);
    private final PreflightConfigService preflightConfigService;
    private final NodeService<DataNodeDto> nodeService;

    private final Supplier<Boolean> resultsCachingSupplier;

    @Inject
    public RunsWithDataNodeDiscoveryProvider(
            PreflightConfigService preflightConfigService,
            NodeService<DataNodeDto> nodeService) {
        this.preflightConfigService = preflightConfigService;
        this.nodeService = nodeService;
        this.resultsCachingSupplier = Suppliers.memoize(this::doGet);
    }

    @Override
    public Boolean get() {
        return resultsCachingSupplier.get();
    }

    private Boolean doGet() {
        final PreflightConfigResult preflightResult = preflightConfigService.getPreflightConfigResult();

        // if preflight is finished, we assume that there will be some datanode registered via node-service.
        if (preflightResult == PreflightConfigResult.FINISHED) {
            final List<URI> discovered = discover();
            if (!discovered.isEmpty()) {
                LOG.debug("Running with DataNode(s).");
                return Boolean.TRUE;
            }
        }
        LOG.debug("Not running with DataNode(s).");
        return Boolean.FALSE;
    }

    private List<URI> discover() {
        return nodeService.allActive().values().stream()
                .map(Node::getTransportAddress)
                .map(URI::create)
                .collect(Collectors.toList());
    }
}
