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

import org.apache.commons.lang3.tuple.Pair;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobScheduleStrategies;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.CronJobSchedule;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreMongoStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoCollections;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.preflight.NodePreflightConfig;
import org.graylog2.cluster.preflight.NodePreflightConfigService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.KeyStoreException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class CertRenewalServiceImpl implements CertRenewalService {
    private static final Logger LOG = LoggerFactory.getLogger(CertRenewalServiceImpl.class);

    private final ClusterConfigService clusterConfigService;
    private final KeystoreMongoStorage keystoreMongoStorage;
    private final NodeService nodeService;
    private final NodePreflightConfigService nodePreflightConfigService;
    private final NotificationService notificationService;
    private final DBJobTriggerService jobTriggerService;
    private final DBJobDefinitionService jobDefinitionService;
    private final JobScheduleStrategies jobScheduleStrategies;
    private final JobSchedulerClock clock;
    private final char[] passwordSecret;

    @Inject
    public CertRenewalServiceImpl(final ClusterConfigService clusterConfigService,
                                  final KeystoreMongoStorage keystoreMongoStorage,
                                  final NodeService nodeService,
                                  final NodePreflightConfigService nodePreflightConfigService,
                                  final NotificationService notificationService,
                                  final DBJobTriggerService jobTriggerService,
                                  final DBJobDefinitionService jobDefinitionService,
                                  final JobScheduleStrategies jobScheduleStrategies,
                                  final JobSchedulerClock clock,
                                  final @Named("password_secret") String passwordSecret) {
        this.clusterConfigService = clusterConfigService;
        this.keystoreMongoStorage = keystoreMongoStorage;
        this.nodeService = nodeService;
        this.nodePreflightConfigService = nodePreflightConfigService;
        this.notificationService = notificationService;
        this.jobTriggerService = jobTriggerService;
        this.jobDefinitionService = jobDefinitionService;
        this.jobScheduleStrategies = jobScheduleStrategies;
        this.clock = clock;
        this.passwordSecret = passwordSecret.toCharArray();
    }

    private RenewalPolicy getRenewalPolicy() {
        return this.clusterConfigService.get(RenewalPolicy.class);
    }

    private boolean needsRenewal(final RenewalPolicy renewalPolicy, final X509Certificate cert) {
        // calculate renewal threshold
        var threshold = calculateThreshold(renewalPolicy.certificateLifetime());

        try {
            cert.checkValidity(threshold);
            cert.checkValidity(nextRenewalJobRun());
        } catch (CertificateExpiredException e) {
            LOG.debug("Certificate about to expire.");
            return true;
        } catch (CertificateNotYetValidException e) {
            LOG.debug("Certificate not yet valid - which is surprising, but ignoring it.");
        }
        return false;
    }

    private Date convertToDateViaInstant(LocalDateTime dateToConvert) {
        return java.util.Date.from(dateToConvert.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Date calculateThreshold(String certificateLifetime) {
        final var lifetime = Duration.parse(certificateLifetime).dividedBy(10);
        var validUntil = clock.now(ZoneId.systemDefault()).plus(lifetime).toLocalDateTime();
        return convertToDateViaInstant(validUntil);
    }

    private Date nextRenewalJobRun() {
// TODO: calculate nextTime from trigger
        //        jobScheduleStrategies.nextTime()
        var nextRenewalJobRun = clock.now(ZoneId.systemDefault()).plusMinutes(30).toLocalDateTime();
        return convertToDateViaInstant(nextRenewalJobRun);
    }

    @Override
    public void checkAllDataNodes() {
        var renewalPolicy = getRenewalPolicy();

        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        activeDataNodes.values().stream()
                .map(node -> {
                    try {
                        return Pair.of(node, keystoreMongoStorage.readKeyStore(new KeystoreMongoLocation(node.getNodeId(), KeystoreMongoCollections.DATA_NODE_KEYSTORE_COLLECTION), passwordSecret).orElse(null));
                    } catch (KeyStoreStorageException e) {
                        LOG.error("Could not read keystore for DataNode: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(p -> p.getRight() != null)
                .map(pair -> {
                    try {
                        return Pair.of(pair.getLeft(), (X509Certificate)pair.getRight().getCertificate(CertConstants.DATANODE_KEY_ALIAS));
                    } catch (KeyStoreException e) {
                        LOG.error("Could not read certificate for DataNode: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(p -> needsRenewal(renewalPolicy, p.getRight()))
                .forEach(pair -> {
                    if(RenewalPolicy.Mode.AUTOMATIC.equals(renewalPolicy.mode())) {
                        var config = nodePreflightConfigService.getPreflightConfigFor(pair.getLeft().getNodeId());
                        nodePreflightConfigService.save(config.toBuilder().state(NodePreflightConfig.State.RENEWAL).build());
                    } else {
                        // TODO: don't send one out, if there is one still open
                        notificationService.fixed(Notification.Type.CERT_NEEDS_RENEWAL, pair.getLeft());
                    }
                });
    }

    @Override
    public void addCheckForRenewalJob() {
        // TODO: check, if the two more fields are needed (see CronJobSchedule Tests)
        final var cronJobSchedule = CronJobSchedule.builder().cronExpression("0,30 * * * *").timezone(null).build();

        final var jobDefinition = JobDefinitionDto.builder().id("cert-renewal-check")
                .title("Certificat Renewal Check")
                .description("Runs periodically to check for certificates that are about to expire and notifies/triggers renewal")
                .build();

        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId("cert-renewal-check")
                .jobDefinitionType(CheckForCertRenewalJob.TYPE_NAME)
                .schedule(cronJobSchedule)
                .status(JobTriggerStatus.RUNNABLE)
                .build();

        jobDefinitionService.save(jobDefinition);
        jobTriggerService.create(trigger);
    }
}
