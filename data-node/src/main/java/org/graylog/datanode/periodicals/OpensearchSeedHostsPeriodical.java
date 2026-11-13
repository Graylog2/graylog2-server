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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.configuration.OpensearchSeedHostsResolver;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Keeps the opensearch discovery seed hosts file in sync with the data nodes registered in the cluster.
 * Data nodes starting at the same time may not see each other during their configuration, this periodical
 * delivers the seed hosts to the running opensearch process as soon as other nodes register.
 */
public class OpensearchSeedHostsPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchSeedHostsPeriodical.class);

    private final Configuration configuration;
    private final OpensearchSeedHostsResolver seedHostsResolver;
    private final OpensearchProcess process;

    @Inject
    public OpensearchSeedHostsPeriodical(Configuration configuration, OpensearchSeedHostsResolver seedHostsResolver, OpensearchProcess process) {
        this.configuration = configuration;
        this.seedHostsResolver = seedHostsResolver;
        this.process = process;
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
        // statically configured seed hosts are used instead of the seed hosts file
        final List<String> discoverySeedHosts = configuration.getOpensearchDiscoverySeedHosts();
        return discoverySeedHosts == null || discoverySeedHosts.isEmpty();
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 5;
    }

    @Override
    public int getPeriodSeconds() {
        return 5;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        process.updateSeedHosts(seedHostsResolver.resolve());
    }
}
