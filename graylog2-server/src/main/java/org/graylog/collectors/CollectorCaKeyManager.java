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
package org.graylog.collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Custom key manager that dynamically retrieves the server and signing certificates. This behavior is required
 * for certificate renewal.
 * <p>
 * Extends {@link X509ExtendedKeyManager} rather than implementing {@link javax.net.ssl.X509KeyManager} because
 * Netty uses {@link javax.net.ssl.SSLEngine}-based handshakes. The JDK wraps a plain {@code X509KeyManager} in
 * an adapter that adds endpoint identification checks; extending the "Extended" variant avoids that wrapper.
 */
@Singleton
public class CollectorCaKeyManager extends X509ExtendedKeyManager {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorCaKeyManager.class);
    private static final String ALIAS = "server";
    private static final Set<String> ED25519_KEY_TYPES = Set.of("EdDSA", "Ed25519");

    private final CollectorCaCache caCache;

    @Inject
    public CollectorCaKeyManager(CollectorCaCache caCache) {
        this.caCache = caCache;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        if (ED25519_KEY_TYPES.contains(keyType)) {
            LOG.debug("Returning <{}> as the server alias for key type <{}>", ALIAS, keyType);
            return ALIAS;
        }
        LOG.debug("Returning null for key type <{}>", keyType);
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        if (ALIAS.equals(alias)) {
            final var serverEntry = caCache.getServer();
            final var signingEntry = caCache.getSigning();
            LOG.debug("Returning certificate chain for alias <{}>: server-cert={} signing-cert={}",
                    alias, serverEntry.fingerprint(), signingEntry.fingerprint());
            return new X509Certificate[]{serverEntry.cert(), signingEntry.cert()};
        }
        LOG.debug("Returning null certificate chain for alias <{}>", alias);
        return null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        if (ALIAS.equals(alias)) {
            final var serverEntry = caCache.getServer();
            LOG.debug("Returning private key for server certificate <{}>", serverEntry.fingerprint());
            return serverEntry.privateKey();
        }
        LOG.debug("Returning null private key for alias <{}>", alias);
        return null;
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return chooseServerAlias(keyType, issuers, null);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return null;
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return null;
    }
}
