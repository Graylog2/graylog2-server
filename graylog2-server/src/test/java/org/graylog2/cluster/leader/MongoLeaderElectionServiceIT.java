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
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.periodical.NodePingThread;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(value = 30)
@ExtendWith(MockitoExtension.class)
class MongoLeaderElectionServiceIT {
    @Mock
    LockService lockService;

    @Mock
    EventBus eventBus;

    @Mock
    NodePingThread nodePingThread;

    @InjectMocks
    MongoLeaderElectionService leaderElectionService;

    @AfterEach
    void tearDown() {
        leaderElectionService.stopAsync().awaitTerminated();
    }

    @Test
    void notLeaderWhenLockCanNeverBeAcquired() {
        assertThat(leaderElectionService.isLeader()).isFalse();

        leaderElectionService.startAsync().awaitRunning();

        // wait until the service executed the run loop at least 2 times
        verify(lockService, timeout(10_000).atLeast(2)).lock(any());

        assertThat(leaderElectionService.isLeader()).isFalse();
    }

    @Test
    void leaderWhenLockIsAcquired() {
        when(lockService.lock(any())).thenReturn(Optional.of(mock(Lock.class)));

        assertThat(leaderElectionService.isLeader()).isFalse();

        leaderElectionService.startAsync().awaitRunning();

        // wait until the service executed the run loop at least 2 times
        verify(lockService, timeout(10_000).atLeast(2)).lock(any());

        assertThat(leaderElectionService.isLeader()).isTrue();
    }

    @Test
    void postsEventWhenLeaderChanges() {
        when(lockService.lock(any())).thenReturn(Optional.of(mock(Lock.class)));

        leaderElectionService.startAsync().awaitRunning();

        verify(eventBus, timeout(10_000)).post(any(LeaderChangedEvent.class));
        assertThat(leaderElectionService.isLeader()).isTrue();

        when(lockService.lock(any())).thenReturn(Optional.empty());
        verify(eventBus, timeout(10_000).times(2)).post(any(LeaderChangedEvent.class));
        assertThat(leaderElectionService.isLeader()).isFalse();
    }

    @Test
    void pausesPollingAfterDowngradeFromLeader() {
        final AtomicInteger lockInvocations = new AtomicInteger();
        final AtomicReference<Lock> lock = new AtomicReference<>();

        when(lockService.lock(any())).then(i -> {
            lockInvocations.incrementAndGet();
            return Optional.ofNullable(lock.get());
        });

        leaderElectionService.startAsync().awaitRunning();
        lock.set(mock(Lock.class));
        await().until(() -> leaderElectionService.isLeader());

        // polling should continue
        int lockCount = lockInvocations.get();
        Uninterruptibles.sleepUninterruptibly(MongoLeaderElectionService.POLLING_INTERVAL.multipliedBy(2));
        assertThat(lockInvocations.get()).isGreaterThan(lockCount);

        lock.set(null);
        await().until(() -> !leaderElectionService.isLeader());

        // polling should have been paused
        lockCount = lockInvocations.get();
        Uninterruptibles.sleepUninterruptibly(MongoLeaderElectionService.POLLING_INTERVAL.multipliedBy(2));
        assertThat(lockInvocations.get()).isEqualTo(lockCount);
    }
}
