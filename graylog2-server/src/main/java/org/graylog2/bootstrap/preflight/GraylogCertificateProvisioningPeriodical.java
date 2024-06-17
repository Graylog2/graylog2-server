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
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;

@Singleton
public class GraylogCertificateProvisioningPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogCertificateProvisioningPeriodical.class);

    private final CaConfiguration configuration;
    private final CaService caService;
    private final CsrSigner csrSigner;
    private final ClusterConfigService clusterConfigService;
    private final String passwordSecret;

    private final CertificateExchange certificateExchange;

    @Inject
    public GraylogCertificateProvisioningPeriodical(final CaService caService,
                                                    final Configuration configuration,
                                                    final CsrSigner csrSigner,
                                                    final ClusterConfigService clusterConfigService,
                                                    final @Named("password_secret") String passwordSecret,
                                                    CertificateExchange certificateExchange) {
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.configuration = configuration;
        this.csrSigner = csrSigner;
        this.clusterConfigService = clusterConfigService;
        this.certificateExchange = certificateExchange;
    }

    private RenewalPolicy getRenewalPolicy() {
        return this.clusterConfigService.get(RenewalPolicy.class);
    }

    @Override
    public void doRun() {
        LOG.debug("checking if there are configuration steps to take care of");

        try {
            // only load nodes that are in a state that need sth done

            final var password = configuration.configuredCaExists()
                    ? configuration.getCaPassword().toCharArray()
                    : passwordSecret.toCharArray();
            final Optional<KeyStore> optKey = caService.loadKeyStore();
            if (optKey.isEmpty()) {
                LOG.debug("No keystore available.");
                return;
            }

            final var renewalPolicy = getRenewalPolicy();
            if (renewalPolicy == null) {
                LOG.debug("No renewal policy available.");
                return;
            }

            optKey.ifPresent(caKeystore -> {
                try {
                    if (renewalPolicy.mode() == RenewalPolicy.Mode.AUTOMATIC) {
                        certificateExchange.signPendingCertificateRequests(request -> signCertificate(request, caKeystore, password, renewalPolicy));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });


        } catch (KeyStoreException | NoSuchAlgorithmException | KeyStoreStorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private CertificateChain signCertificate(CertificateSigningRequest request, KeyStore caKeystore, char[] password, RenewalPolicy renewalPolicy) {
        try {
            // TODO: this should be hidden behind a CertificateAuthority abstraction, we should never deal with private keys and passwords here
            var caPrivateKey = (PrivateKey) caKeystore.getKey(CA_KEY_ALIAS, password);
            var caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);
            var cert = csrSigner.sign(caPrivateKey, caCertificate, request.request(), renewalPolicy);
            final List<X509Certificate> caCertificates = List.of(caCertificate);
            return new CertificateChain(cert, caCertificates);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
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
        return true;
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
        return 2;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }

}
