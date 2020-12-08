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
package org.graylog.plugins.sidecar.periodical;

import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog.plugins.sidecar.system.SidecarConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PurgeExpiredSidecarsThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PurgeExpiredSidecarsThread.class);

    private final SidecarService sidecarService;
    private final SidecarConfiguration sidecarConfiguration;

    @Inject
    public PurgeExpiredSidecarsThread(SidecarService sidecarService,
                                      ClusterConfigService clusterConfigService) {
        this.sidecarService = sidecarService;
        this.sidecarConfiguration = clusterConfigService.getOrDefault(SidecarConfiguration.class, SidecarConfiguration.defaultConfiguration());
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
    public boolean masterOnly() {
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
        return 60 * 10;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Period inactiveThreshold = this.sidecarConfiguration.sidecarInactiveThreshold();
        final int expiredSidecars = sidecarService.markExpired(inactiveThreshold, "Received no ping signal from sidecar");
        LOG.debug("Marked {} sidecars as inactive.", expiredSidecars);
        final int purgedSidecars = sidecarService.destroyExpired(this.sidecarConfiguration.sidecarExpirationThreshold());
        LOG.debug("Purged {} inactive sidecars.", purgedSidecars);
    }
}
