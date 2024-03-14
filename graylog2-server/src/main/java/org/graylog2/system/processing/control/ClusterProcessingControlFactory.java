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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.nodes.ServerNodeDto;
import org.graylog2.rest.RemoteInterfaceProvider;

import static org.graylog2.Configuration.INSTALL_HTTP_CONNECTION_TIMEOUT;
import static org.graylog2.Configuration.INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL;
import static org.graylog2.Configuration.INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES;

public class ClusterProcessingControlFactory {
    protected final RemoteInterfaceProvider remoteInterfaceProvider;
    protected final NodeService<ServerNodeDto> nodeService;
    protected final Duration connectionTimeout;
    private final Duration bufferDrainInterval;
    private final int maxBufferDrainRetries;

    @Inject
    public ClusterProcessingControlFactory(final RemoteInterfaceProvider remoteInterfaceProvider,
                                           final NodeService<ServerNodeDto> nodeService,
                                           @Named(INSTALL_HTTP_CONNECTION_TIMEOUT) final Duration connectionTimeout,
                                           @Named(INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL) final Duration bufferDrainInterval,
                                           @Named(INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES) final int maxBufferDrainRetries) {
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        this.nodeService = nodeService;
        this.connectionTimeout = connectionTimeout;
        this.bufferDrainInterval = bufferDrainInterval;
        this.maxBufferDrainRetries = maxBufferDrainRetries;
    }

    public ClusterProcessingControl<RemoteProcessingControlResource> create(String authorizationToken) {
        return new ClusterProcessingControl<>(authorizationToken, remoteInterfaceProvider, nodeService, connectionTimeout, bufferDrainInterval, maxBufferDrainRetries);
    }
}
