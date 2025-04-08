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
package org.graylog2;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.graylog2.shared.bindings.GuiceInjectorHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.function.Consumer;

public class TestHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final String baseUri;

    public TestHttpServer(HttpServer server, String baseUri) {
        this.server = server;
        this.baseUri = baseUri;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * This method will execute your test action against a running instance of the test server.
     * @param builderAction Adapt the test server to your needs. Primarly for registering resources.
     * @param testAction your test action that should be executed against a running test server.
     */
    public static void withServer(Consumer<Builder> builderAction, Consumer<TestHttpServer> testAction) {
        final Builder builder = TestHttpServer.builder();
        builderAction.accept(builder);
        try (final TestHttpServer testServer = builder
                .build()
                .start()) {
            testAction.accept(testServer);
        }
    }

    public URI getBaseUri() {
        return URI.create(baseUri);
    }

    public TestHttpServer start() {
        try {
            server.start();
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        server.shutdownNow();
    }

    @Override
    public void close() {
        stop();
    }

    public static class Builder {
        private final ResourceConfig rc = new ResourceConfig();
        private KeyStore keyStore;
        private String keystorePassword;


        public Builder configureResources(Consumer<ResourceConfig> consumer) {
            consumer.accept(rc);
            return this;
        }

        public Builder registerResource(Class<?> resourceClass) {
            rc.register(resourceClass);
            return this;
        }

        public Builder registerResource(Object component) {
            rc.register(component);
            return this;
        }


        public Builder withSsl(KeyStore keyStore, String password) {
            this.keyStore = keyStore;
            this.keystorePassword = password;
            return this;
        }

        private static byte[] keystoreToBytes(KeyStore keyStore, String password) {
            try {
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                keyStore.store(byteArrayOutputStream, password.toCharArray());
                return byteArrayOutputStream.toByteArray();
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                throw new RuntimeException(e);
            }
        }

        private static int findFreePort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private SSLEngineConfigurator buildSSLConfig() {
            if (keyStore != null && keystorePassword != null) {
                SSLContextConfigurator sslCon = new SSLContextConfigurator();
                sslCon.setKeyStoreBytes(keystoreToBytes(keyStore, keystorePassword));
                sslCon.setKeyStorePass(keystorePassword);
                sslCon.setTrustStoreBytes(keystoreToBytes(keyStore, keystorePassword));
                sslCon.setTrustStorePass(keystorePassword);
                return new SSLEngineConfigurator(sslCon).setClientMode(false).setNeedClientAuth(false);
            }
            return null;
        }

        public TestHttpServer build() {
            GuiceInjectorHolder.createInjector(Collections.emptyList());

            final int freePort = findFreePort();
            final SSLEngineConfigurator sslConfig = buildSSLConfig();
            final boolean secure = sslConfig != null;

            String baseUri = (secure ? "https" : "http") + "://localhost:" + freePort;

            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), rc, secure, sslConfig);
            return new TestHttpServer(server, baseUri);
        }
    }
}
