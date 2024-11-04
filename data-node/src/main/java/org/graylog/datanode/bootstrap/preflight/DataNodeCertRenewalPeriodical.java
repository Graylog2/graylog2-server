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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.opensearch.CsrRequester;
import org.graylog.security.certutil.CertificateAuthorityChangedEvent;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
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
    public static final Duration PERIODICAL_DURATION = Duration.ofSeconds(2);

    private final DatanodeKeystore datanodeKeystore;
    private final Supplier<RenewalPolicy> renewalPolicySupplier;

    private final CsrRequester csrRequester;

    private final Supplier<Boolean> isServerInPreflightMode;

    /**
     * We need to differentiate between interval of the task scheduling and actual automatic CSR request triggering.
     * This prevents flooding the server with CSR requests, which would then lead to repeated certificate signing and
     * repeated opensearch process reboots. We want to check if the CSR is needed very often,
     * but we don't want to trigger it many times in a row.
     */
    private final RateLimiter rateLimiter;

    @Inject
    public DataNodeCertRenewalPeriodical(DatanodeKeystore datanodeKeystore, ClusterConfigService clusterConfigService, CsrRequester csrRequester, PreflightConfigService preflightConfigService) {
        this(datanodeKeystore, () -> clusterConfigService.get(RenewalPolicy.class), csrRequester, () -> isInPreflight(preflightConfigService));
    }

    protected DataNodeCertRenewalPeriodical(DatanodeKeystore datanodeKeystore, Supplier<RenewalPolicy> renewalPolicySupplier, CsrRequester csrRequester, Supplier<Boolean> isServerInPreflightMode) {
        this.datanodeKeystore = datanodeKeystore;
        this.renewalPolicySupplier = renewalPolicySupplier;
        this.csrRequester = csrRequester;
        this.isServerInPreflightMode = isServerInPreflightMode;
        // Max 1 CSR every minute
        this.rateLimiter = RateLimiter.create(1.0 / Duration.ofMinutes(5).toSeconds());
    }

    @Override
    public void doRun() {
        if (isServerInPreflightMode.get()) {
            // we don't want to automatically trigger CSRs during preflight, don't run it if the preflight is still not finished or skipped
            LOG.debug("Datanode still in preflight mode, skipping cert renewal task");
            return;
        }

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

    private static boolean isInPreflight(PreflightConfigService preflightConfigService) {
        return preflightConfigService.getPreflightConfigResult() != PreflightConfigResult.FINISHED;
    }

    private void manualRenewal() {
        LOG.debug("Manual renewal, ignoring on the datanode side for now");
    }

    private void automaticRenewal() {
        if (rateLimiter.tryAcquire()) {
            csrRequester.triggerCertificateSigningRequest();
        }
    }

    private boolean needsNewCertificate(RenewalPolicy renewalPolicy) {
        final Date expiration = datanodeKeystore.getCertificateExpiration();
        return expiration == null || expiresSoon(expiration, renewalPolicy);
    }

    private boolean expiresSoon(Date expiration, RenewalPolicy renewalPolicy) {
        Duration threshold = renewalPolicy.getRenewalThreshold();
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
