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
package org.graylog.security.certutil;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreMongoStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoCollections;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.graylog.security.certutil.CheckForCertRenewalJob.RENEWAL_JOB_ID;

@Singleton
public class CertRenewalServiceImpl implements CertRenewalService {
    private static final Logger LOG = LoggerFactory.getLogger(CertRenewalServiceImpl.class);

    private final ClusterConfigService clusterConfigService;
    private final KeystoreMongoStorage keystoreMongoStorage;
    private final NodeService nodeService;
    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final NotificationService notificationService;
    private final DBJobTriggerService jobTriggerService;
    private final JobSchedulerClock clock;
    private final CaService caService;
    private final char[] passwordSecret;

    // TODO: convert to config?
    private long CERT_RENEWAL_THRESHOLD_PERCENTAGE = 10;

    @Inject
    public CertRenewalServiceImpl(final ClusterConfigService clusterConfigService,
                                  final KeystoreMongoStorage keystoreMongoStorage,
                                  final NodeService nodeService,
                                  final DataNodeProvisioningService dataNodeProvisioningService,
                                  final NotificationService notificationService,
                                  final DBJobTriggerService jobTriggerService,
                                  final CaService caService,
                                  final JobSchedulerClock clock,
                                  final @Named("password_secret") String passwordSecret) {
        this.clusterConfigService = clusterConfigService;
        this.keystoreMongoStorage = keystoreMongoStorage;
        this.nodeService = nodeService;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.notificationService = notificationService;
        this.jobTriggerService = jobTriggerService;
        this.clock = clock;
        this.caService = caService;
        this.passwordSecret = passwordSecret.toCharArray();
    }

    @VisibleForTesting
    CertRenewalServiceImpl(final JobSchedulerClock clock) {
        this(null, null, null, null, null, null, null, clock, "dummy");
    }

    private RenewalPolicy getRenewalPolicy() {
        return this.clusterConfigService.get(RenewalPolicy.class);
    }

    boolean needsRenewal(final DateTime nextRenewal, final RenewalPolicy renewalPolicy, final X509Certificate cert) {
        // calculate renewal threshold
        var threshold = calculateThreshold(renewalPolicy.certificateLifetime());

        try {
            cert.checkValidity(threshold);
            cert.checkValidity(nextRenewal.toDate());
        } catch (CertificateExpiredException e) {
            LOG.debug("Certificate about to expire.");
            return true;
        } catch (CertificateNotYetValidException e) {
            LOG.debug("Certificate not yet valid - which is surprising, but ignoring it.");
        }
        return false;
    }

    Date convertToDateViaInstant(LocalDateTime dateToConvert) {
        return Date.from(dateToConvert.atZone(ZoneId.systemDefault()).toInstant());
    }

    Date calculateThreshold(String certificateLifetime) {
        final var lifetime = Duration.parse(certificateLifetime).dividedBy(CERT_RENEWAL_THRESHOLD_PERCENTAGE);
        var validUntil = clock.now(ZoneId.systemDefault()).plus(lifetime).toLocalDateTime();
        return convertToDateViaInstant(validUntil);
    }

    @Override
    public void checkCertificatesForRenewal() {
        final var renewalPolicy = getRenewalPolicy();

        // a renewal policy has to be present to check for outdated certificates
        if (renewalPolicy != null) {
            checkDataNodesCertificatesForRenewal(renewalPolicy);
            checkCaCertificatesForRenewal(renewalPolicy);
        }
    }

    private DateTime getNextRenewal() {
        return jobTriggerService.getOneForJob(RENEWAL_JOB_ID).map(JobTriggerDto::nextTime).orElse(DateTime.now(DateTimeZone.UTC).plusMinutes(30));
    }

    protected void checkCaCertificatesForRenewal(final RenewalPolicy renewalPolicy) {
        try {
            final var keystore = caService.loadKeyStore();
            if(keystore.isPresent()) {
                final var ks = keystore.get();
                final var nextRenewal = getNextRenewal();
                final var rootCert = ks.getCertificate("root");
                if(needsRenewal(nextRenewal, renewalPolicy, (X509Certificate) rootCert)) {
                    notificationService.fixed(Notification.Type.CERTIFICATE_NEEDS_RENEWAL, "root cert");
                }
                final var caCert = ks.getCertificate("ca");
                if(needsRenewal(nextRenewal, renewalPolicy, (X509Certificate) caCert)) {
                    notificationService.fixed(Notification.Type.CERTIFICATE_NEEDS_RENEWAL, "ca cert");
                }
            }
        } catch (KeyStoreException | KeyStoreStorageException | NoSuchAlgorithmException e) {
            LOG.error("Could not read CA keystore: {}", e.getMessage());
        }
    }

    private Pair<Node, KeyStore> loadKeyStoreForNode(Node node) {
        try {
            return Pair.of(node, keystoreMongoStorage.readKeyStore(new KeystoreMongoLocation(node.getNodeId(), KeystoreMongoCollections.DATA_NODE_KEYSTORE_COLLECTION), passwordSecret).orElse(null));
        } catch (KeyStoreStorageException e) {
            return Pair.of(node, null);
        }
    }

    private Pair<Node, X509Certificate> getCertificateForNode(Pair<Node, KeyStore> pair) {
        if(pair.getRight() == null) {
            return Pair.of(pair.getLeft(), null);
        }

        try {
            return Pair.of(pair.getLeft(), (X509Certificate)pair.getRight().getCertificate(CertConstants.DATANODE_KEY_ALIAS));
        } catch (KeyStoreException e) {
            return Pair.of(pair.getLeft(), null);
        }
    }

    private List<Pair<Node, X509Certificate>> findNodesAndCertificates() {
        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        return activeDataNodes.values().stream()
                .map(this::loadKeyStoreForNode)
                .map(this::getCertificateForNode)
                .filter(p -> p.getRight() != null)
                .toList();
    }

    protected List<Node> findNodesThatNeedCertificateRenewal(final RenewalPolicy renewalPolicy) {
        final var nextRenewal = getNextRenewal();
        return findNodesAndCertificates().stream()
                .filter(p -> needsRenewal(nextRenewal, renewalPolicy, p.getRight()))
                .map(Pair::getLeft).toList();
    }

    @Override
    public void initiateRenewalForNode(final String nodeId) {
        // write new state to MongoDB so that the DataNode picks it up and generates a new CSR request
        var config = dataNodeProvisioningService.getPreflightConfigFor(nodeId);
        dataNodeProvisioningService.save(config.toBuilder().state(DataNodeProvisioningConfig.State.CONFIGURED).build());
    }

    @Override
    public List<Triple<Node, DataNodeProvisioningConfig, X509Certificate>> findNodes() {
        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        return activeDataNodes.values().stream()
                .map(this::loadKeyStoreForNode)
                .map(this::getCertificateForNode)
                .map(this::addDataNodeProvisioningConfig)
                .toList();
    }

    private Triple<Node, DataNodeProvisioningConfig, X509Certificate> addDataNodeProvisioningConfig(Pair<Node, X509Certificate> pair) {
        return Triple.of(pair.getKey(), dataNodeProvisioningService.getPreflightConfigFor(pair.getKey().getNodeId()), pair.getValue());
    }

    private void notifyManualRenewalForNode(final List<Node> nodes) {
        final var key = String.join(",", nodes.stream().map(Node::getNodeId).toList());
        if(!notificationService.isFirst(Notification.Type.CERTIFICATE_NEEDS_RENEWAL)) {
            notificationService.fixed(Notification.Type.CERTIFICATE_NEEDS_RENEWAL);
        }
        Notification notification = notificationService.buildNow()
                    .addType(Notification.Type.CERTIFICATE_NEEDS_RENEWAL)
                    .addSeverity(Notification.Severity.URGENT)
                    .addKey(key)
                    .addDetail("nodes", key);
        notificationService.publishIfFirst(notification);
    }

    protected void checkDataNodesCertificatesForRenewal(final RenewalPolicy renewalPolicy) {
        final var nodes = findNodesThatNeedCertificateRenewal(renewalPolicy);
        if(RenewalPolicy.Mode.AUTOMATIC.equals(renewalPolicy.mode())) {
            nodes.forEach(node -> initiateRenewalForNode(node.getNodeId()));
        } else {
            notifyManualRenewalForNode(nodes);
        }
    }
}
