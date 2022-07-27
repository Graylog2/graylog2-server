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
package org.graylog2.cluster.leader;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog2.Configuration;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.periodical.NodePingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;

@Singleton
public class AutomaticLeaderElectionService extends AbstractExecutionThreadService implements LeaderElectionService {
    private static final String RESOURCE_NAME = "cluster-leader";
    private static final Logger log = LoggerFactory.getLogger(AutomaticLeaderElectionService.class);

    public static final java.time.Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(2);

    private final LockService lockService;
    private final EventBus eventBus;
    private final NodePingThread nodePingThread;
    private final Duration pollingInterval;
    private final Duration lockTTL;

    private volatile boolean isLeader = false;
    private Thread executionThread;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Lock> optionalLock;

    @Inject
    public AutomaticLeaderElectionService(Configuration configuration,
                                          LockService lockService,
                                          EventBus eventBus,
                                          NodePingThread nodePingThread) {
        this.pollingInterval = configuration.getLeaderElectionLockPollingInterval();
        this.lockTTL = configuration.getLockServiceLockTTL();
        this.lockService = lockService;
        this.eventBus = eventBus;
        this.nodePingThread = nodePingThread;
        this.optionalLock = Optional.empty();
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        executionThread = Thread.currentThread();
    }

    @Override
    protected void triggerShutdown() {
        executionThread.interrupt();
    }

    @Override
    protected void shutDown() {
        isLeader = false;
        optionalLock.flatMap(lockService::unlock).ifPresent(l -> log.info("Gave up leader lock on shutdown"));
    }

    @Override
    public boolean isLeader() {
        return isLeader;
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            try {
                runIteration();
            } catch (Throwable t) {
                log.error("Exception while acquiring/renewing leader lock.", t);
            }
        }
    }

    private void runIteration() {
        final boolean wasLeader = isLeader;

        try {
            optionalLock = lockService.lock(RESOURCE_NAME, null);
            isLeader = optionalLock.isPresent();
        } catch (Exception e) {
            log.error("Unable to acquire/renew leader lock.", e);

            if (wasLeader) {
                log.error("Failed to renew leader lock. Forcing fallback to follower role.");
                isLeader = false;
            }
        }

        if (wasLeader != isLeader) {
            handleLeaderChange(isLeader);
        }

        pauseBeforeNextIteration(wasLeader && !isLeader);
    }

    private void pauseBeforeNextIteration(boolean isDowngrade) {
        try {
            if (isDowngrade) {
                log.info("Pausing leader-lock acquisition attempts for {} after downgrade from leader.", lockTTL);
                Thread.sleep(lockTTL.toMillis());
                log.info("Resuming leader-lock acquisition attempts every {}.", pollingInterval);
            } else {
                Thread.sleep(pollingInterval.toMillis());
            }
        } catch (InterruptedException e) {
            // OK, we are shutting down. Don't' restore interrupted flag, so we can release the lock in shutdown()
        }
    }

    private void handleLeaderChange(boolean isLeader) {
        if (isLeader) {
            log.info("Leader changed. This node is now the leader.");
        } else {
            log.error("Leader changed. This node lost the leader role. This should not happen. The node will " +
                    "attempt to gracefully transition to assuming a follower role.");
        }

        // Ensure the nodes collection is up to date before we publish the event
        try {
            nodePingThread.doRun();
        } catch (Exception e) {
            log.error("Unable to trigger update of nodes collection.");
        }
        eventBus.post(new LeaderChangedEvent());
    }
}
