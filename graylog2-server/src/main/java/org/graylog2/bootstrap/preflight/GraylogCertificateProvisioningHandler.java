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
package org.graylog2.bootstrap.preflight;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.security.certutil.CaConfiguration;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog2.Configuration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;

/**
 * This is the graylog-server handler that reacts to certificate signing requests and returns signed certificates.
 */
public class GraylogCertificateProvisioningHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogCertificateProvisioningHandler.class);
    private final CaConfiguration configuration;
    private final CaService caService;
    private final CsrSigner csrSigner;
    private final ClusterConfigService clusterConfigService;
    private final String passwordSecret;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public GraylogCertificateProvisioningHandler(final CaService caService,
                                                 final Configuration configuration,
                                                 final CsrSigner csrSigner,
                                                 final ClusterConfigService clusterConfigService,
                                                 final @Named("password_secret") String passwordSecret,
                                                 final EventBus eventBus,
                                                 final ClusterEventBus clusterEventBus
    ) {
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.configuration = configuration;
        this.csrSigner = csrSigner;
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = clusterEventBus;
        eventBus.register(this);
    }

    private Optional<RenewalPolicy> getRenewalPolicy() {
        return Optional.ofNullable(this.clusterConfigService.get(RenewalPolicy.class));
    }

    @Subscribe
    public void signCsrListener(CertificateSigningRequestEvent event) {
        LOG.info("Received CertificateSigningRequestEvent for node " + event.nodeId());
        getKeyStore().ifPresentOrElse(caKeystore -> {
            getRenewalPolicy().ifPresentOrElse(renewalPolicy -> {
                try {
                    var caPrivateKey = (PrivateKey) caKeystore.getKey(CA_KEY_ALIAS, getCAPassword());
                    var caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);
                    var cert = csrSigner.sign(caPrivateKey, caCertificate, event.decodeCsr(), renewalPolicy);
                    final CertificateChain certChain = new CertificateChain(cert, List.of(caCertificate));

                    LOG.info("Posted CertificateSignedEvent for node " + event.nodeId());
                    clusterEventBus.post(CertificateSignedEvent.fromCertChain(event.nodeId(), certChain));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, () -> LOG.warn("Couldn't sign certificate, no renewal policy configured"));
        }, () -> LOG.warn("Couldn't sign certificate, no CA configured"));
    }

    @Nonnull
    private char[] getCAPassword() {
        return configuration.configuredCaExists()
                ? configuration.getCaPassword().toCharArray()
                : passwordSecret.toCharArray();
    }

    private Optional<KeyStore> getKeyStore() {
        try {
            return caService.loadKeyStore();
        } catch (KeyStoreException | KeyStoreStorageException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
