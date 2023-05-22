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

import org.graylog.security.certutil.cert.storage.CertMongoStorage;
import org.graylog.security.certutil.cert.storage.CertStorage;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.storage.CsrMongoStorage;
import org.graylog2.cluster.NodePreflightConfig;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Singleton
public class GraylogPreflightGeneratePeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogPreflightGeneratePeriodical.class);

    private final NodePreflightConfigService nodePreflightConfigService;

    private final CsrMongoStorage csrStorage;
    private final CertStorage certMongoStorage;

    private String caKeystoreFilename = "datanode-ca.p12";
    private Integer DEFAULT_VALIDITY = 90;
    private static final String DEFAULT_PASSWORD = "admin";

    @Inject
    public GraylogPreflightGeneratePeriodical(final NodePreflightConfigService nodePreflightConfigService,
                                              final CsrMongoStorage csrStorage,
                                              final CertMongoStorage certMongoStorage) {
        this.nodePreflightConfigService = nodePreflightConfigService;
        this.csrStorage = csrStorage;
        this.certMongoStorage = certMongoStorage;
    }

    @Override
    public void doRun() {
        LOG.debug("checking if there are configuration steps to take care of");
        final Path caKeystorePath = Path.of(caKeystoreFilename);

        try {
            char[] password = DEFAULT_PASSWORD.toCharArray();
            KeyStore caKeystore = KeyStore.getInstance("PKCS12");
            caKeystore.load(new FileInputStream(caKeystorePath.toFile()), password);

            var caPrivateKey = (PrivateKey) caKeystore.getKey("ca", password);
            var caCertificate = (X509Certificate) caKeystore.getCertificate("ca");

            // TODO: delete?
//            final X509Certificate rootCertificate = (X509Certificate) caKeystore.getCertificate("root");

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
                                certMongoStorage.writeCert(cert, c.nodeId());
                            }
                        } catch (Exception e) {
                            LOG.error("Could not sign CSR: " + e.getMessage(), e);
                            nodePreflightConfigService.save(c.toBuilder().state(NodePreflightConfig.State.ERROR).errorMsg(e.getMessage()).build());
                        }
                    });

        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException |
                 UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
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
        return 2;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }
}
