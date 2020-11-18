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
package org.graylog2.periodical;

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

public class ClusterIdGeneratorPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterIdGeneratorPeriodical.class);

    private final ClusterConfigService clusterConfigService;

    @Inject
    public ClusterIdGeneratorPeriodical(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
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
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        if(clusterConfigService.get(ClusterId.class) == null) {
            ClusterId clusterId = ClusterId.create(UUID.randomUUID().toString());
            clusterConfigService.write(clusterId);

            LOG.debug("Generated cluster ID {}", clusterId.clusterId());
        }
    }
}
