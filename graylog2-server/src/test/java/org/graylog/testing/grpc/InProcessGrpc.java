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
package org.graylog.testing.grpc;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Runs gRPC services on in-process servers, so tests exercise real gRPC call semantics (interceptors, contexts,
 * status propagation) without opening network ports.
 * <p>
 * Instances are injected by {@link InProcessGrpcExtension}, which shuts down all servers and channels afterwards.
 */
public class InProcessGrpc implements AutoCloseable {
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final List<Server> servers = new ArrayList<>();
    private final List<ManagedChannel> channels = new ArrayList<>();

    InProcessGrpc() {
    }

    /**
     * Starts an in-process server for the given service and returns a channel connected to it.
     * <p>
     * Interceptors follow the same ordering as {@link io.grpc.ServerBuilder#intercept(ServerInterceptor)}: the
     * interceptor given last is called first.
     */
    public ManagedChannel start(BindableService service, ServerInterceptor... interceptors) throws IOException {
        final var serverName = InProcessServerBuilder.generateName();

        servers.add(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(service, interceptors))
                .build()
                .start());

        final var channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        channels.add(channel);
        return channel;
    }

    /**
     * Shuts down all channels and servers and waits for their termination.
     *
     * @throws IllegalStateException if a channel or server doesn't terminate within the timeout
     */
    @Override
    public void close() throws InterruptedException {
        // Initiate the shutdown for everything first, so an interrupted wait doesn't leave anything running.
        channels.forEach(ManagedChannel::shutdownNow);
        servers.forEach(Server::shutdownNow);

        final List<String> notTerminated = new ArrayList<>();
        for (final ManagedChannel channel : channels) {
            if (!channel.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                notTerminated.add(channel.toString());
            }
        }
        for (final Server server : servers) {
            if (!server.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                notTerminated.add(server.toString());
            }
        }
        channels.clear();
        servers.clear();

        if (!notTerminated.isEmpty()) {
            throw new IllegalStateException(f("Not terminated within %d seconds: %s",
                    SHUTDOWN_TIMEOUT_SECONDS, String.join(", ", notTerminated)));
        }
    }
}
