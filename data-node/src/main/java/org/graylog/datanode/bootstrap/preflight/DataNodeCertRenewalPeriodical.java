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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.opensearch.CsrRequester;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class DataNodeCertRenewalPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeCertRenewalPeriodical.class);
    private static final long CERT_RENEWAL_THRESHOLD_PERCENTAGE = 10;
    public static final Duration PERIODICAL_DURATION = Duration.ofMinutes(30);

    private final DatanodeKeystore datanodeKeystore;
    private final Supplier<RenewalPolicy> renewalPolicySupplier;

    private final CsrRequester csrRequester;

    @Inject
    public DataNodeCertRenewalPeriodical(DatanodeKeystore datanodeKeystore, ClusterConfigService clusterConfigService, CsrRequester csrRequester) {
        this(datanodeKeystore, () -> clusterConfigService.get(RenewalPolicy.class), csrRequester);
    }

    protected DataNodeCertRenewalPeriodical(DatanodeKeystore datanodeKeystore, Supplier<RenewalPolicy> renewalPolicySupplier, CsrRequester csrRequester) {
        this.datanodeKeystore = datanodeKeystore;
        this.renewalPolicySupplier = renewalPolicySupplier;
        this.csrRequester = csrRequester;
    }


    @Override
    public void doRun() {
        // always check if there are any certificates that we can accept
        getRenewalPolicy()
                .filter(this::needsNewCertificate)
                .ifPresent(renewalPolicy -> {
                    switch (renewalPolicy.mode()) {
                        case AUTOMATIC -> automaticRenewal();
                        case MANUAL -> manualRenewal();
                    }
                });

    }

    private void manualRenewal() {
        LOG.debug("Manual renewal, ignoring on the datanode side for now");
    }

    private void automaticRenewal() {
        csrRequester.triggerCsr();
    }

    private boolean needsNewCertificate(RenewalPolicy renewalPolicy) {
        final Date expiration = datanodeKeystore.getCertificateExpiration();
        return expiration == null || expiresSoon(expiration, renewalPolicy);
    }

    private boolean expiresSoon(Date expiration, RenewalPolicy renewalPolicy) {
        final Duration threshold = Duration.parse(renewalPolicy.certificateLifetime()).dividedBy(CERT_RENEWAL_THRESHOLD_PERCENTAGE);
        final Instant renewalMoment = expiration.toInstant()
                .minus(threshold)
                .minus(PERIODICAL_DURATION);
        final boolean expiresSoon = Instant.now().isAfter(renewalMoment);
        if (expiresSoon) {
            LOG.info("Datanode certificate will be renewed now, expiring soon (" + expiration + ")");
        }
        return expiresSoon;
    }

    private Optional<RenewalPolicy> getRenewalPolicy() {
        return Optional.ofNullable(renewalPolicySupplier.get());
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
        return (int) PERIODICAL_DURATION.toSeconds();
    }
}
