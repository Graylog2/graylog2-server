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
import org.graylog2.cluster.lock.LockService;
import org.graylog2.periodical.NodePingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;

@Singleton
public class MongoLeaderElectionService extends AbstractExecutionThreadService implements LeaderElectionService {
    private static final String RESOURCE_NAME = "cluster-leader";
    private static final Logger log = LoggerFactory.getLogger(MongoLeaderElectionService.class);

    public static final Duration POLLING_INTERVAL = Duration.ofSeconds(2);
    public static final Duration LOCK_TTL = Duration.ofMinutes(1);

    private final LockService lockService;
    private final EventBus eventBus;
    private final NodePingThread nodePingThread;

    private volatile boolean isLeader = false;
    private volatile Thread executionThread;

    @Inject
    public MongoLeaderElectionService(LockService lockService, EventBus eventBus, NodePingThread nodePingThread) {
        this.lockService = lockService;
        this.eventBus = eventBus;
        this.nodePingThread = nodePingThread;
    }

    @Override
    protected void startUp() throws Exception {
        this.executionThread = Thread.currentThread();
    }

    @Override
    protected void triggerShutdown() {
        executionThread.interrupt();
    }

    @Override
    public boolean isLeader() {
        return isLeader;
    }

    @Override
    protected void run() throws Exception {
        long lastSuccess = 0;

        while (isRunning()) {
            final boolean wasLeader = isLeader;

            try {
                isLeader = lockService.lock(RESOURCE_NAME).isPresent();
                lastSuccess = System.nanoTime();
            } catch (Exception e) {
                log.error("Unable to acquire/renew leader lock.", e);

                final Duration timeSinceLastSuccess = Duration.ofNanos(System.nanoTime() - lastSuccess);
                if (wasLeader && timeSinceLastSuccess.compareTo(LOCK_TTL) >= 0) {
                    log.error("Failed for {} to renew leader lock. Forcing fallback to follower role.",
                            timeSinceLastSuccess);
                    isLeader = false;
                }
            }

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
                if (wasLeader && !isLeader) {
                    log.info("Pausing leader-lock acquisition attempts for {} after downgrade from leader.", LOCK_TTL);
                    Thread.sleep(LOCK_TTL.toMillis());
                    log.info("Resuming leader-lock acquisition attempts every {}.", POLLING_INTERVAL);
                } else {
                    Thread.sleep(POLLING_INTERVAL.toMillis());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
