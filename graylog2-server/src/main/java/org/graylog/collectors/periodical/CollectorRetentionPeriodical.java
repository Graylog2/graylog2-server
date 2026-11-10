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
package org.graylog.collectors.periodical;

import jakarta.inject.Inject;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;

/**
 * Deletes collector instances that haven't been seen for longer than the expiration threshold and
 * truncates the fleet transaction log accordingly.
 */
public class CollectorRetentionPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorRetentionPeriodical.class);
    // Grace period during which unprocessed transaction markers outlive the collector expiration threshold.
    private static final Duration TXN_LOG_SAFETY_MARGIN = Duration.ofDays(30);
    private static final int MIN_TXNS_TO_KEEP = 100;

    private final CollectorInstanceService collectorInstanceService;
    private final CollectorsConfigService collectorsConfigService;
    private final FleetTransactionLogService txnLogService;
    private final Clock clock;

    @Inject
    public CollectorRetentionPeriodical(CollectorInstanceService collectorInstanceService,
                                                    CollectorsConfigService collectorsConfigService,
                                                    FleetTransactionLogService txnLogService,
                                                    Clock clock) {
        this.collectorInstanceService = collectorInstanceService;
        this.collectorsConfigService = collectorsConfigService;
        this.txnLogService = txnLogService;
        this.clock = clock;
    }

    @Override
    public void doRun() {
        final var expirationThreshold = collectorsConfigService.getOrDefault().collectorExpirationThreshold();
        final var purgedInstances = collectorInstanceService.deleteExpired(expirationThreshold);
        LOG.debug("Purged {} expired collector instances.", purgedInstances);

        final var txnCutoff = clock.instant().minus(expirationThreshold.plus(TXN_LOG_SAFETY_MARGIN));
        final long purgedTxns = txnLogService.purgeMarkers(txnCutoff, MIN_TXNS_TO_KEEP);
        LOG.debug("Purged {} transactions.", purgedTxns);
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
        return 60;
    }

    @Override
    public int getPeriodSeconds() {
        return 5 * 60;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
