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

import java.time.Instant;

public class PurgeCollectorFleetTransactionLogPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PurgeCollectorFleetTransactionLogPeriodical.class);

    private final FleetTransactionLogService fleetTransactionLogService;
    private final CollectorInstanceService collectorInstanceService;
    private final CollectorsConfigService collectorsConfigService;

    @Inject
    public PurgeCollectorFleetTransactionLogPeriodical(FleetTransactionLogService fleetTransactionLogService,
                                                       CollectorInstanceService collectorInstanceService,
                                                       CollectorsConfigService collectorsConfigService) {
        this.fleetTransactionLogService = fleetTransactionLogService;
        this.collectorInstanceService = collectorInstanceService;
        this.collectorsConfigService = collectorsConfigService;
    }

    @Override
    public void doRun() {
        final var config = collectorsConfigService.getOrDefault();
        final var timeCutoff = Instant.now().minus(config.collectorTransactionLogRetentionThreshold());
        final long minActiveSeq = collectorInstanceService.getMinLastProcessedTxnSeq();
        final long deleted = fleetTransactionLogService.deleteOldMarkers(timeCutoff, minActiveSeq);
        if (deleted > 0) {
            LOG.info("Purged {} old transaction log markers (retention threshold: {}, min active seq: {}).",
                    deleted, config.collectorTransactionLogRetentionThreshold(), minActiveSeq);
        } else {
            LOG.debug("No old transaction log markers to purge.");
        }
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
        return 120;
    }

    @Override
    public int getPeriodSeconds() {
        return 86400;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
