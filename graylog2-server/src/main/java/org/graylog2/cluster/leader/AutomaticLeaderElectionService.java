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
import org.graylog2.cluster.lock.LockService;
import org.graylog2.periodical.NodePingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;

@Singleton
public class AutomaticLeaderElectionService extends AbstractExecutionThreadService implements LeaderElectionService {
    private static final String RESOURCE_NAME = "cluster-leader";
    private static final Logger log = LoggerFactory.getLogger(AutomaticLeaderElectionService.class);

    public static final java.time.Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(2);

    private final LockService lockService;
    private final EventBus eventBus;
    private final NodePingThread nodePingThread;
    private final Duration leaderElectionLockPollingInterval;

    private volatile boolean isLeader = false;
    private Thread executionThread;

    @Inject
    public AutomaticLeaderElectionService(Configuration configuration,
                                          LockService lockService,
                                          EventBus eventBus,
                                          NodePingThread nodePingThread) {
        this.leaderElectionLockPollingInterval = configuration.getLeaderElectionLockPollingInterval();
        this.lockService = lockService;
        this.eventBus = eventBus;
        this.nodePingThread = nodePingThread;
    }

    @Override
    public boolean isLeader() {
        return isLeader;
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        executionThread = Thread.currentThread();
    }

    @Override
    protected void run() throws Exception {

        while (isRunning()) {
            final boolean wasLeader = isLeader;

            // TODO: failure handling
            isLeader = lockService.lock(RESOURCE_NAME).isPresent();

            if (wasLeader != isLeader) {
                if (isLeader) {
                    log.info("Leader changed. This node is now the leader.");
                } else {
                    log.error("Leader changed. This node lost the leader role. This should not happen. The node will " +
                            "attempt to gracefully transition to assuming a follower role.");
                }

                // Ensure the nodes collection is up to date before we publish the event
                nodePingThread.doRun();
                eventBus.post(new LeaderChangedEvent());
            }

            try {
                //noinspection BusyWait
                Thread.sleep(leaderElectionLockPollingInterval.toMillis());
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void triggerShutdown() {
        executionThread.interrupt();
    }

    @Override
    public void giveUpLeader() {
        lockService.unlock(RESOURCE_NAME).ifPresent(l -> log.info("Gave up leader lock on shutdown"));
    }
}
