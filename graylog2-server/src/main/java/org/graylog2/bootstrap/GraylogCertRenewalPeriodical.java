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
package org.graylog2.bootstrap;

import org.graylog.security.certutil.CaConfiguration;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.cert.storage.CertChainMongoStorage;
import org.graylog.security.certutil.cert.storage.CertChainStorage;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.storage.CsrMongoStorage;
import org.graylog2.Configuration;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.preflight.NodePreflightConfig;
import org.graylog2.cluster.preflight.NodePreflightConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CaService.DEFAULT_VALIDITY;

public class GraylogCertRenewalPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogCertRenewalPeriodical.class);

    private final NodePreflightConfigService nodePreflightConfigService;
    private final NodeService nodeService;

    private final CaConfiguration configuration;
    private final CsrMongoStorage csrStorage;
    private final CertChainStorage certMongoStorage;
    private final CaService caService;
    private final String passwordSecret;

    @Inject
    public GraylogCertRenewalPeriodical(final NodePreflightConfigService nodePreflightConfigService,
                                              final CsrMongoStorage csrStorage,
                                              final CertChainMongoStorage certMongoStorage,
                                              final CaService caService,
                                              final Configuration configuration,
                                              final NodeService nodeService,
                                              final @Named("password_secret") String passwordSecret) {
        this.nodePreflightConfigService = nodePreflightConfigService;
        this.csrStorage = csrStorage;
        this.certMongoStorage = certMongoStorage;
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.configuration = configuration;
        this.nodeService = nodeService;
    }

    @Override
    public void doRun() {
        LOG.info("checking if there are configuration steps to take care of");

        try {
            final var password = configuration.configuredCaExists() ? configuration.getCaPassword().toCharArray() : passwordSecret.toCharArray();
            Optional<KeyStore> optKey = caService.loadKeyStore();
            if(optKey.isEmpty()) {
                LOG.warn("No keystore available.");
                return;
            }

            KeyStore caKeystore = optKey.get();
            var caPrivateKey = (PrivateKey) caKeystore.getKey("ca", password);
            var caCertificate = (X509Certificate) caKeystore.getCertificate("ca");

            var rootCertificate = (X509Certificate) caKeystore.getCertificate("root");

            nodePreflightConfigService.streamAll()
                    .filter(c -> NodePreflightConfig.State.CSR.equals(c.state()))
                    .forEach(c -> {
                        try {
                            var csr = csrStorage.readCsr(c.nodeId());
                            if (csr.isEmpty()) {
                                LOG.error("Node in CSR state, but no CSR present : " + c.nodeId());
                                nodePreflightConfigService.save(c.toBuilder()
                                        .state(NodePreflightConfig.State.ERROR)
                                        .errorMsg("Node in CSR state, but no CSR present")
                                        .build());
                            } else {
                                var cert = CsrSigner.sign(caPrivateKey, caCertificate, csr.get(), c.validFor() != null ? c.validFor() : DEFAULT_VALIDITY);
                                //TODO: assumptions about the chain, to contain 2 CAs, named "ca" and "root"...
                                final List<X509Certificate> caCertificates = List.of(caCertificate, rootCertificate);
                                certMongoStorage.writeCertChain(new CertificateChain(cert, caCertificates), c.nodeId());
                            }
                        } catch (Exception e) {
                            LOG.error("Could not sign CSR: " + e.getMessage(), e);
                            nodePreflightConfigService.save(c.toBuilder().state(NodePreflightConfig.State.ERROR).errorMsg(e.getMessage()).build());
                        }
                    });

            nodePreflightConfigService.streamAll()
                    .filter(c -> NodePreflightConfig.State.STORED.equals(c.state()))
                    .forEach(c -> nodePreflightConfigService.save(c.toBuilder().state(NodePreflightConfig.State.CONNECTED).build()));

        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreStorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 5;
    }

    @Override
    public int getPeriodSeconds() {
        return 5;
    }
}
