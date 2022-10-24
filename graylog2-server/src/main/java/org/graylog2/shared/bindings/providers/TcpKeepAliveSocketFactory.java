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
package org.graylog2.shared.bindings.providers;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * This socket factory wrapper sets the SO_KEEPALIVE flag for every created socket. The timeouts for the socket
 * depend on the configuration of the underlying operating system. See {@link #configure(Socket)}.
 *
 * Caution! The {@code shouldSetTcpKeepAlive} configures only new sockets. It's possible that there
 * will be still some older sockets waiting in the thread pool and will be reused with the original tcp keep-alive
 * setting before the change.
 */
public class TcpKeepAliveSocketFactory extends SocketFactory {

    private final SocketFactory delegate;
    private final Predicate<Socket> shouldSetTcpKeepAlive;

    public
    TcpKeepAliveSocketFactory(SocketFactory delegate, Predicate<Socket> shouldSendTcpKeepAliveProbe) {
        this.delegate = Objects.requireNonNull(delegate);
        this.shouldSetTcpKeepAlive = shouldSendTcpKeepAliveProbe;
    }

    @Override
    public Socket createSocket() throws IOException {
        return configure(delegate.createSocket());
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return configure(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return configure(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return configure(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return configure(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket configure(Socket socket) throws SocketException {
        if (shouldSetTcpKeepAlive.test(socket)) {
            socket.setKeepAlive(true);
        }
        return socket;
    }
}
