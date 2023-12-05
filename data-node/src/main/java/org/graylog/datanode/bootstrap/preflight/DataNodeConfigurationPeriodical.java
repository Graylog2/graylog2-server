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
package org.graylog.datanode.bootstrap.preflight;

import com.google.common.collect.ImmutableList;
import org.bouncycastle.operator.OperatorException;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.cert.storage.CertChainMongoStorage;
import org.graylog.security.certutil.cert.storage.CertChainStorage;
import org.graylog.security.certutil.csr.CertificateAndPrivateKeyMerger;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.csr.storage.CsrMongoStorage;
import org.graylog.security.certutil.keystore.storage.SmartKeystoreStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoCollections;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class DataNodeConfigurationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeConfigurationPeriodical.class);

    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;
    private final PrivateKeyEncryptedFileStorage privateKeyEncryptedStorage;
    private final CsrMongoStorage csrStorage;
    private final CsrGenerator csrGenerator;
    private final CertChainStorage certMongoStorage;
    private final CertificateAndPrivateKeyMerger certificateAndPrivateKeyMerger;
    private final SmartKeystoreStorage keystoreStorage;
    private final char[] passwordSecret;

    @Inject
    public DataNodeConfigurationPeriodical(final DataNodeProvisioningService dataNodeProvisioningService,
                                           final NodeService<DataNodeDto> nodeService,
                                           final NodeId nodeId,
                                           final CsrMongoStorage csrStorage,
                                           final CsrGenerator csrGenerator,
                                           final CertChainMongoStorage certMongoStorage,
                                           final CertificateAndPrivateKeyMerger certificateAndPrivateKeyMerger,
                                           final SmartKeystoreStorage keystoreStorage,
                                           final @Named("password_secret") String passwordSecret,
                                           final DatanodeConfiguration datanodeConfiguration) throws IOException {
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.csrStorage = csrStorage;
        this.csrGenerator = csrGenerator;
        this.certMongoStorage = certMongoStorage;
        this.certificateAndPrivateKeyMerger = certificateAndPrivateKeyMerger;
        this.keystoreStorage = keystoreStorage;
        // TODO: merge with real storage
        this.privateKeyEncryptedStorage = new PrivateKeyEncryptedFileStorage(datanodeConfiguration.datanodeDirectories().createConfigurationFile(Path.of("privateKey.cert")));
        this.passwordSecret = passwordSecret.toCharArray();
    }

    @Override
    public void doRun() {
        LOG.debug("checking if this DataNode is supposed to take configuration steps.");
        var cfg = dataNodeProvisioningService.getPreflightConfigFor(nodeId.getNodeId());
        if (cfg.isEmpty()) {
            // write default config if none exists for this node
            writeInitialProvisioningConfig();
            return;
        }
        cfg.ifPresent(c -> {
            final var state = c.state();
            if (state == null) {
                return;
            }
            switch (state) {
                case CONFIGURED -> writeCsr(c);
                case SIGNED -> readSignedCertificate(c);
            }
        });
    }

    private void readSignedCertificate(DataNodeProvisioningConfig cfg) {
        if (cfg.certificate() == null) {
            LOG.error("Config entry in signed state, but no certificate data present in Mongo");
        } else {
            try {
                final Optional<CertificateChain> certificateChain = certMongoStorage.readCertChain(nodeId.getNodeId());
                if (certificateChain.isPresent()) {
                    final char[] secret = passwordSecret;
                    KeyStore nodeKeystore = certificateAndPrivateKeyMerger.merge(
                            certificateChain.get(),
                            privateKeyEncryptedStorage,
                            secret,
                            secret,
                            CertConstants.DATANODE_KEY_ALIAS
                    );

                    final KeystoreMongoLocation location = new KeystoreMongoLocation(nodeId.getNodeId(), KeystoreMongoCollections.DATA_NODE_KEYSTORE_COLLECTION);
                    keystoreStorage.writeKeyStore(location, nodeKeystore, secret, secret);

                    //should be in one transaction, but we miss transactions...
                    dataNodeProvisioningService.changeState(nodeId.getNodeId(), DataNodeProvisioningConfig.State.STORED);
                }
            } catch (Exception ex) {
                LOG.error("Config entry in signed state, but wrong certificate data present in Mongo");
            }
        }
    }

    private void writeCsr(DataNodeProvisioningConfig cfg) {
        try {
            final var node = nodeService.byNodeId(nodeId);
            final var altNames = ImmutableList.<String>builder()
                    .addAll(Optional.ofNullable(cfg.altNames()).orElse(Collections.emptyList()))
                    .addAll(determineAltNames())
                    .build();
            final var csr = csrGenerator.generateCSR(passwordSecret, node.getHostname(), altNames, privateKeyEncryptedStorage);
            csrStorage.writeCsr(csr, nodeId.getNodeId());
            LOG.info("created CSR for this node");
        } catch (CSRGenerationException | IOException | NodeNotFoundException | OperatorException ex) {
            LOG.error("error generating a CSR: " + ex.getMessage(), ex);
            dataNodeProvisioningService.save(cfg.asError(ex.getMessage()));
        }
    }

    private void writeInitialProvisioningConfig() {
        dataNodeProvisioningService.save(DataNodeProvisioningConfig.builder()
                .nodeId(nodeId.getNodeId())
                .state(DataNodeProvisioningConfig.State.UNCONFIGURED)
                .build());
    }

    private Iterable<String> determineAltNames() {
        return Stream.of("127.0.0.1", "::1")
                .map(this::reverseLookup)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @SuppressForbidden("Deliberate use of InetAddress#getHostName")
    private String reverseLookup(String ipAddress) {
        try {
            final var inetAddress = InetAddress.getByName(ipAddress);
            final var reverseLookup = inetAddress.getHostName();
            return reverseLookup.equals(ipAddress) ? null : reverseLookup;
        } catch (Exception e) {
            return null;
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
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }
}
