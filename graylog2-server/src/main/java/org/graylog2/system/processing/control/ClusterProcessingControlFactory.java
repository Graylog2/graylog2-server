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
package org.graylog2.system.processing.control;

import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog2.Configuration.INSTALL_HTTP_CONNECTION_TIMEOUT;
import static org.graylog2.Configuration.INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL;
import static org.graylog2.Configuration.INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES;
import static org.graylog2.shared.utilities.StringUtils.f;

public class ClusterProcessingControlFactory<I extends ClusterProcessingControlFactory.ClusterProcessingControl> {
    private static final String OUTPUT_RATE_METRIC_NAME = "org.graylog2.throughput.output.1-sec-rate";
    protected final RemoteInterfaceProvider remoteInterfaceProvider;
    protected final NodeService nodeService;
    protected final Duration connectionTimeout;
    private final Duration bufferDrainInterval;
    private final int maxBufferDrainRetries;

    @Inject
    public ClusterProcessingControlFactory(final RemoteInterfaceProvider remoteInterfaceProvider,
                                           final NodeService nodeService,
                                           @Named(INSTALL_HTTP_CONNECTION_TIMEOUT) final Duration connectionTimeout,
                                           @Named(INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL) final Duration bufferDrainInterval,
                                           @Named(INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES) final int maxBufferDrainRetries) {
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        this.nodeService = nodeService;
        this.connectionTimeout = connectionTimeout;
        this.bufferDrainInterval = bufferDrainInterval;
        this.maxBufferDrainRetries = maxBufferDrainRetries;
    }

    public ClusterProcessingControlFactory<I>.ClusterProcessingControl create(String authorizationToken) {
        return new ClusterProcessingControl(authorizationToken);
    }

    /**
     * Provides ability to execute Illuminate processing control operations on all nodes in a Graylog Cluster.
     * See public methods for supported operations.
     */
    public class ClusterProcessingControl {
        private final Logger LOG = LoggerFactory.getLogger(ClusterProcessingControl.class);
        protected final String authorizationToken;

        @Inject
        public ClusterProcessingControl(String authorizationToken) {
            this.authorizationToken = authorizationToken;
        }

        public void pauseProcessing() {
            runOnAllActiveNodes("pause processing", RemoteProcessingControlResource::pauseProcessing, true);
        }

        protected <R> Map<String, R> runOnAllActiveNodes(
                String operationName,
                Function<RemoteProcessingControlResource, Call<R>> callRemoteResource,
                boolean stopOnFirstException
        ) {
            final Map<String, R> result = new HashMap<>();
            final List<ClusterProcessingControlException> exceptions = new ArrayList<>();
            printNodeDebugInfo();
            nodeService.allActive().entrySet().forEach(entry -> {
                final Node nodeValue = entry.getValue();
                try {
                    LOG.info("Attempting to call '{}' on node [{}].", operationName, nodeValue.getNodeId());
                    final RemoteProcessingControlResource remoteProcessingControlResource = processingControlResource(entry);
                    final Response<R> response = callRemoteResource.apply(remoteProcessingControlResource).execute();
                    if (!response.isSuccessful()) {
                        final String message = f("Unable to call '%s' on node [%s] code [%s] body [%s]",
                                operationName, nodeValue.getNodeId(),
                                response.code(), response.body());
                        LOG.error("Unable to call '{}' on node [{}] code [{}] body [{}].",
                                operationName, nodeValue.getNodeId(),
                                response.code(), response.body());
                        throw new ClusterProcessingControlException(message);
                    }
                    result.put(entry.getKey(), response.body());
                    LOG.info("Successfully called '{}' on node [{}].", operationName, nodeValue.getNodeId());
                } catch (Exception e) {
                    if (e instanceof ClusterProcessingControlException) {
                        exceptions.add((ClusterProcessingControlException) e);
                    } else {
                        final String message = f("Unable to call '%s' on node [%s]", operationName, nodeValue.getNodeId());
                        LOG.error(message, e);
                        exceptions.add(new ClusterProcessingControlException(message, e));
                    }

                    if (stopOnFirstException) {
                        throw exceptions.get(0);
                    }
                }
            });

            if (!exceptions.isEmpty()) {
                throw exceptions.get(0);
            }

            return result;
        }

        protected RemoteProcessingControlResource processingControlResource(Map.Entry<String, Node> entry) {
            return remoteInterfaceProvider.get(entry.getValue(),
                    this.authorizationToken, RemoteProcessingControlResource.class,
                    java.time.Duration.ofSeconds(connectionTimeout.toSeconds()));
        }

        public void waitForEmptyBuffers() throws OutputBufferDrainFailureException {
            printNodeDebugInfo();
            final Retryer<NodeOperationResult> retryer = RetryerBuilder.<NodeOperationResult>newBuilder()
                    .retryIfResult(value -> !value.success)
                    .withWaitStrategy(WaitStrategies.fixedWait(bufferDrainInterval.toSeconds(), TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.stopAfterAttempt(maxBufferDrainRetries))
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            if (attempt.getAttemptNumber() > 1) {
                                LOG.info("Checking again for empty output buffers (attempt #{}).", attempt.getAttemptNumber());
                            }
                        }
                    })
                    .build();
            try {
                retryer.call(() -> {
                    final Map<String, Double> nodeOutputRateMap = runOnAllActiveNodes("fetching output rate metric value",
                            res -> res.getMetric(OUTPUT_RATE_METRIC_NAME), true)
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Double) entry.getValue().get("value")));
                    final boolean allZero = new HashSet<>(nodeOutputRateMap.values()).stream()
                            .allMatch(this::isOutputRateCloseToZero);
                    final Set<String> nonZeroNodes = nodeOutputRateMap
                            .entrySet()
                            .stream()
                            .filter(e -> !isOutputRateCloseToZero(e.getValue()))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
                    if (allZero) {
                        LOG.info("Output buffer is now empty on all nodes.");
                    } else {
                        LOG.info("Output rate has not yet reached zero on nodes [{}].", nonZeroNodes);
                    }
                    return new NodeOperationResult(allZero, nonZeroNodes);
                });
            } catch (RetryException e) {
                final String message = f("The [%s] rate failed to reach zero on all nodes in [%s] with [%s] retries. Giving up. " +
                                "This is configurable with the [%s] and [%s] configuration properties", OUTPUT_RATE_METRIC_NAME,
                        bufferDrainInterval.toSeconds(), maxBufferDrainRetries, INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL,
                        INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES);
                LOG.error(message);
                throw new OutputBufferDrainFailureException(bufferDrainInterval.toSeconds(), maxBufferDrainRetries,
                        tryGetExceptionNodes(e));
            } catch (Exception e) {
                throw new ClusterProcessingControlException("Failed to request node output rate on all nodes.", e);
            }
        }

        /**
         * Try to retrieve the nodes that have a non-zero output rate from the RetryException.
         * This should succeed with the current implementation.
         */
        protected static Set<String> tryGetExceptionNodes(RetryException e) {
            try {
                return ((NodeOperationResult) e.getLastFailedAttempt().get()).nonZeroOutputRateNodeIds();
            } catch (ExecutionException ex) {
                return Collections.emptySet();
            }
        }

        public record NodeOperationResult(boolean success, Set<String> nonZeroOutputRateNodeIds) {
        }

        /**
         * The output rate is the number of messages per second that are being written to OpenSearch (usually a
         * whole number followed by some meaningless decimals - e.g. 100.01 messages/second).
         * A value < 1 is effectively zero. The rate might become very small, but not zero in some cases,
         * so this method accounts for that condition.
         */
        protected boolean isOutputRateCloseToZero(double outputRate) {
            return outputRate < 0.0001;
        }

        public void resumeGraylogMessageProcessing() {
            LOG.info("Attempting to resume processing on all nodes...");
            runOnAllActiveNodes("resume processing", RemoteProcessingControlResource::resumeProcessing, false);
            LOG.info("Done resuming processing on all nodes.");
        }

        protected void printNodeDebugInfo() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The Graylog cluster contains the following nodes:");
                nodeService.allActive().entrySet().forEach((entry) -> {
                    final Node node = entry.getValue();
                    LOG.debug("Node ID [{}] Transport Address [{}] Last Seen [{}]", node.getNodeId(), node.getTransportAddress(), node.getLastSeen());
                });
            }
        }
    }
}
