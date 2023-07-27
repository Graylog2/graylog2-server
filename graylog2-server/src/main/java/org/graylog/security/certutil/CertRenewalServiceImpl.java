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
import org.graylog.scheduler.DBCustomJobDefinitionService;
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
    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final NotificationService notificationService;
    private final DBJobTriggerService jobTriggerService;
    private final DBJobDefinitionService jobDefinitionService;
    private final DBCustomJobDefinitionService customJobDefinitionService;
    private final JobScheduleStrategies jobScheduleStrategies;
    private final JobSchedulerClock clock;
    private final char[] passwordSecret;

    // TODO: convert to config?
    private long CERT_RENEWAL_THRESHOLD_PERCENTAGE = 10;

    public static final String RENEWAL_JOB_ID = "cert-renewal-check";

    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id(RENEWAL_JOB_ID) // This is a system entity and the ID MUST NOT change!
            .title("Certificat Renewal Check")
            .description("Runs periodically to check for certificates that are about to expire and notifies/triggers renewal")
            .config(CheckForCertRenewalJob.Config.builder().build())
            .build();

    @Inject
    public CertRenewalServiceImpl(final ClusterConfigService clusterConfigService,
                                  final KeystoreMongoStorage keystoreMongoStorage,
                                  final NodeService nodeService,
                                  final DataNodeProvisioningService dataNodeProvisioningService,
                                  final NotificationService notificationService,
                                  final DBJobTriggerService jobTriggerService,
                                  final DBJobDefinitionService jobDefinitionService,
                                  final DBCustomJobDefinitionService customJobDefinitionService,
                                  final JobScheduleStrategies jobScheduleStrategies,
                                  final JobSchedulerClock clock,
                                  final @Named("password_secret") String passwordSecret) {
        this.clusterConfigService = clusterConfigService;
        this.keystoreMongoStorage = keystoreMongoStorage;
        this.nodeService = nodeService;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.notificationService = notificationService;
        this.jobTriggerService = jobTriggerService;
        this.jobDefinitionService = jobDefinitionService;
        this.customJobDefinitionService = customJobDefinitionService;
        this.jobScheduleStrategies = jobScheduleStrategies;
        this.clock = clock;
        this.passwordSecret = passwordSecret.toCharArray();
    }

    @VisibleForTesting
    CertRenewalServiceImpl(final JobSchedulerClock clock) {
        this(null, null, null, null, null, null, null, null, null, clock, "dummy");
    }

    @Override
    public RenewalPolicy getRenewalPolicy() {
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
    public void checkAllDataNodes() {
        final var renewalPolicy = getRenewalPolicy();

        // a renewal policy has to be present to check for outdated certificates
        if(renewalPolicy == null) {
            return;
        }

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
                .filter(p -> {
                    var nextRenewal = jobTriggerService.getOneForJob(RENEWAL_JOB_ID).map(JobTriggerDto::nextTime).orElse(DateTime.now(DateTimeZone.UTC).plusMinutes(30));
                    return needsRenewal(nextRenewal, renewalPolicy, p.getRight());
                })
                .forEach(pair -> {
                    if(RenewalPolicy.Mode.AUTOMATIC.equals(renewalPolicy.mode())) {
                        // write new state to MongoDB so that the DataNode picks it up and generates a new CSR request
                        var config = dataNodeProvisioningService.getPreflightConfigFor(pair.getLeft().getNodeId());
                        dataNodeProvisioningService.save(config.toBuilder().state(DataNodeProvisioningConfig.State.CONFIGURED).build());
                    } else {
                        // TODO: send notification - don't send one out, if there is one still open
                        notificationService.fixed(Notification.Type.CERT_NEEDS_RENEWAL, pair.getLeft());
                    }
                });
    }

    @Override
    public void scheduleJob() {
        // TODO: check, if the two more fields are needed (see CronJobSchedule Tests)
        final var cronJobSchedule = CronJobSchedule.builder().cronExpression("0,30 * * * *").timezone(null).build();

        final var jobDefinition = customJobDefinitionService.findOrCreate(DEFINITION_INSTANCE);

        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId(jobDefinition.id())
                .jobDefinitionType(CheckForCertRenewalJob.TYPE_NAME)
                .schedule(cronJobSchedule)
                .status(JobTriggerStatus.RUNNABLE)
                .build();

        jobTriggerService.create(trigger);
    }
}
