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
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurgeExpiredCollectorInstancesPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PurgeExpiredCollectorInstancesPeriodical.class);

    private final CollectorInstanceService collectorInstanceService;
    private final CollectorsConfigService collectorsConfigService;

    @Inject
    public PurgeExpiredCollectorInstancesPeriodical(CollectorInstanceService collectorInstanceService,
                                                    CollectorsConfigService collectorsConfigService) {
        this.collectorInstanceService = collectorInstanceService;
        this.collectorsConfigService = collectorsConfigService;
    }

    @Override
    public void doRun() {
        final var expirationThreshold = collectorsConfigService.getOrDefault().collectorExpirationThreshold();
        final long purged = collectorInstanceService.deleteExpired(expirationThreshold);
        LOG.debug("Purged {} expired collector instances.", purged);
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
