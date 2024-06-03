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
package org.graylog.datanode.periodicals;

import com.google.common.eventbus.EventBus;
import jakarta.inject.Inject;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

public class DatanodeCertificateRenewalPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeCertificateRenewalPeriodical.class);

    private final long CERT_RENEWAL_THRESHOLD_PERCENTAGE = 10;

    private final DatanodeKeystore keystore;
    private final ClusterConfigService clusterConfigService;
    private final EventBus eventBus;
    private final NodeId nodeId;

    @Inject
    public DatanodeCertificateRenewalPeriodical(
            DatanodeKeystore datanodeKeystore,
            ClusterConfigService clusterConfigService,
            EventBus eventBus,
            NodeId nodeId
    ) {
        this.keystore = datanodeKeystore;
        this.clusterConfigService = clusterConfigService;
        this.eventBus = eventBus;
        this.nodeId = nodeId;
    }


    @Override
    public void doRun() {
        try {
            if (keystore.hasSignedCertificate()) {
                getRenewalPolicy()
                        .filter(renewalPolicy -> isRenewalNeeded(renewalPolicy, keystore.getCertificateExpiration()))
                        .ifPresent(rp -> triggerCSR());
            }
        } catch (DatanodeKeystoreException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean isRenewalNeeded(RenewalPolicy rp, Date expiration) {
        final Duration threshold = Duration.parse(rp.certificateLifetime()).dividedBy(CERT_RENEWAL_THRESHOLD_PERCENTAGE);
        final Instant renewalTriggerMoment = expiration.toInstant().minus(threshold);
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        //return now.isAfter(renewalTriggerMoment.atZone(ZoneId.systemDefault()));
        return true;
    }

    private void triggerCSR() {
        LOG.info("Datanode certificate expires soon, triggering certificate signing request now");
        eventBus.post(DataNodeLifecycleEvent.create(nodeId.getNodeId(), DataNodeLifecycleTrigger.REQUEST_CERTIFICATE));
    }


    private Optional<RenewalPolicy> getRenewalPolicy() {
        return Optional.ofNullable(this.clusterConfigService.get(RenewalPolicy.class));
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
        return (int) Duration.ofMinutes(2).toSeconds();
    }

    @NotNull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
