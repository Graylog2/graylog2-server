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

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

/**
 * This {@link SSLSocketFactory} wrapper sets the SO_KEEPALIVE flag for every created socket. The timeouts for the socket
 * depend on the configuration of the underlying operating system. See {@link #configure(Socket)}.
 */
public class TcpKeepAliveSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;

    public TcpKeepAliveSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = Objects.requireNonNull(delegate);
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

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return configure(delegate.createSocket(s, host, port, autoClose));
    }

    private Socket configure(Socket socket) throws SocketException {
        socket.setKeepAlive(true);
        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }
}
