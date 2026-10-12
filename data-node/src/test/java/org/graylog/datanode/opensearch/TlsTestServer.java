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
package org.graylog.datanode.opensearch;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Minimal TLS server presenting a single, fixed certificate - just enough for
 * {@link CertificateReloadVerifier} to complete a handshake against and read the leaf certificate back.
 */
public final class TlsTestServer implements AutoCloseable {
    private final SSLServerSocket serverSocket;
    private volatile boolean running = true;

    public TlsTestServer(KeyStore serverKeyStore, char[] keyPassword) throws Exception {
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(serverKeyStore, keyPassword);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        this.serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory()
                .createServerSocket(0, 50, InetAddress.getLoopbackAddress());
        final Thread acceptThread = new Thread(this::acceptLoop, "certificate-reload-verifier-test-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running) {
            try (SSLSocket client = (SSLSocket) serverSocket.accept()) {
                client.startHandshake();
            } catch (IOException e) {
                // socket got closed (test tearing down) or handshake failed - either way, nothing to do
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        serverSocket.close();
    }
}
