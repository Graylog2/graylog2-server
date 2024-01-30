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

import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchStatusException;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.core.MainResponse;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Optional;

@Singleton
public class OpensearchNodeHeartbeat extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchNodeHeartbeat.class);
    private final OpensearchProcess process;

    @Inject
    public OpensearchNodeHeartbeat(OpensearchProcess process) {
        this.process = process;
    }

    @Override
    // This method is "synchronized" because we are also calling it directly in AutomaticLeaderElectionService
    public synchronized void doRun() {
        if (!process.isInState(ProcessState.TERMINATED) && !process.isInState(ProcessState.WAITING_FOR_CONFIGURATION)
                && !process.isInState(ProcessState.REMOVED)) {

            final Optional<RestHighLevelClient> restClient = process.restClient();
            if (restClient.isPresent()) {
                try {
                    final MainResponse health = restClient.get()
                            .info(RequestOptions.DEFAULT);
                    onNodeResponse(process, health);
                } catch (IOException | OpenSearchStatusException e) {
                    onRestError(process, e);
                }
            }
        }
    }

    private void onNodeResponse(OpensearchProcess process, MainResponse nodeResponse) {
        process.onEvent(ProcessEvent.HEALTH_CHECK_OK);
    }

    private void onRestError(OpensearchProcess process, Exception e) {
        process.onEvent(ProcessEvent.HEALTH_CHECK_FAILED);
        LOG.warn("Opensearch REST api of process {} unavailable. Cause: {}", process.processInfo().process().pid(), e.getMessage());
    }

    @Nonnull
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
        return false;
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
        return 10;
    }
}
