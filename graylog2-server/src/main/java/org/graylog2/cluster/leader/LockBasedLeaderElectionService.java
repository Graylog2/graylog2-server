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

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Singleton
public class LockBasedLeaderElectionService extends AbstractExecutionThreadService implements LeaderElectionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DefaultLockingTaskExecutor lockingTaskExecutor;
    private final MongoLockProvider lockProvider;
    private MongoConnection mongoConnection;
    private volatile boolean isLeader;


    @Inject
    public LockBasedLeaderElectionService(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
        lockProvider = new MongoLockProvider(mongoConnection.getMongoDatabase());
        lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);
        isLeader = false;
    }

    @Override
    protected void run() throws Exception {

        Optional<SimpleLock> lock = Optional.empty();

        while (isRunning()) {

            if (lock.isPresent()) {
                lock = lock.get().extend(Duration.ofSeconds(30), Duration.ofSeconds(5));

                isLeader = lock.isPresent();
                if (!isLeader) {
                    log.info("Lost LEADER lock.");
                }

            } else {
                final LockConfiguration lockConfiguration = new LockConfiguration(Instant.now(), "isLeader", Duration.ofSeconds(30), Duration.ofSeconds(5));
                lock = lockProvider.lock(lockConfiguration);
                isLeader = lock.isPresent();

                if (isLeader) {
                    log.info("Acquired LEADER lock.");
                }
            }

            Thread.sleep(3000);
        }
    }

    public boolean isLeader() {
        return isLeader;
    }
}
