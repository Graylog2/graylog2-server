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

import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Suppliers;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class IndexerDiscoveryProvider implements Provider<List<URI>> {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerDiscoveryProvider.class);

    public static final URI DEFAULT_INDEXER_HOST = URI.create("http://127.0.0.1:9200");

    private final List<URI> hosts;
    private final PreflightConfigService preflightConfigService;
    private final NodeService<DataNodeDto> nodeService;

    private final Supplier<List<URI>> resultsCachingSupplier;

    private final int connectionAttempts;
    private final Duration delayBetweenAttempts;


    @Inject
    public IndexerDiscoveryProvider(
            @Named("elasticsearch_hosts") List<URI> hosts,
            @Named("datanode_startup_connection_attempts") int connectionAttempts,
            @Named("datanode_startup_connection_delay") Duration delayBetweenAttempts,
            PreflightConfigService preflightConfigService,
            NodeService<DataNodeDto> nodeService) {
        this.hosts = hosts;
        this.connectionAttempts = connectionAttempts;
        this.delayBetweenAttempts = delayBetweenAttempts;
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
            try {
                //noinspection UnstableApiUsage
                return RetryerBuilder.<List<URI>>newBuilder()
                        .retryIfResult(List::isEmpty)
                        .withRetryListener(new RetryListener() {
                            @Override
                            public void onRetry(Attempt attempt) {
                                if (!attempt.hasResult() || isEmptyList(attempt.getResult())) {
                                    if (connectionAttempts == 0) {
                                        LOG.info("Datanode is not available. Retry #{}", attempt.getAttemptNumber());
                                    } else {
                                        LOG.info("Datanode is not available. Retry #{}/{}", attempt.getAttemptNumber(), connectionAttempts);
                                    }
                                }
                            }
                        })
                        .withWaitStrategy(WaitStrategies.fixedWait(delayBetweenAttempts.getQuantity(), delayBetweenAttempts.getUnit()))
                        .withStopStrategy((connectionAttempts == 0) ? StopStrategies.neverStop() : StopStrategies.stopAfterAttempt(connectionAttempts))
                        .build().call(this::discover);
            } catch (ExecutionException | RetryException e) {
                LOG.error("Unable to retrieve Datanode connection: ", e);
                throw new IllegalStateException("Unable to retrieve Datanode connection", e);
            }
        }

        // if there are no configured hosts and the preflight never has run or was skipped, we should fallback
        // to our old default localhost:9200 to preserve backwards compatibility.
        LOG.info("No indexer hosts configured, using fallback {}", DEFAULT_INDEXER_HOST);
        return Collections.singletonList(DEFAULT_INDEXER_HOST);

    }

    private boolean isEmptyList(Object result) {
        return result instanceof List<?> && ((List<?>)result).isEmpty();
    }

    private List<URI> discover() {
        return nodeService.allActive().values().stream()
                .map(Node::getTransportAddress)
                .map(URI::create)
                .collect(Collectors.toList());
    }
}
