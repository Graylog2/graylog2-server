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
import org.graylog2.Configuration;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.periodical.NodePingThread;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(value = 10)
@ExtendWith(MockitoExtension.class)
class AutomaticLeaderElectionServiceTest {
    @Mock
    LockService lockService;

    @Mock
    EventBus eventBus;

    @Mock
    NodePingThread nodePingThread;

    @Mock
    Configuration configuration;

    AutomaticLeaderElectionService leaderElectionService;

    @BeforeEach
    void setUp() {
        when(configuration.getLeaderElectionLockPollingInterval()).thenReturn(Duration.ofMillis(100));
        when(configuration.getLockServiceLockTTL()).thenReturn(Duration.ofMillis(500));
        leaderElectionService = new AutomaticLeaderElectionService(configuration, lockService, eventBus, nodePingThread);
    }

    @AfterEach
    void tearDown() {
        leaderElectionService.stopAsync().awaitTerminated();
    }

    @Test
    void notLeaderWhenLockCanNeverBeAcquired() {
        assertThat(leaderElectionService.isLeader()).isFalse();

        leaderElectionService.startAsync().awaitRunning();

        // wait until the service executed the run loop at least 2 times
        verify(lockService, timeout(10_000).atLeast(2)).lock(any(), isNull());

        assertThat(leaderElectionService.isLeader()).isFalse();
    }

    @Test
    void leaderWhenLockIsAcquired() {
        when(lockService.lock(any(), isNull())).thenReturn(Optional.of(mock(Lock.class)));

        assertThat(leaderElectionService.isLeader()).isFalse();

        leaderElectionService.startAsync().awaitRunning();

        // wait until the service executed the run loop at least 2 times
        verify(lockService, timeout(10_000).atLeast(2)).lock(any(), isNull());

        assertThat(leaderElectionService.isLeader()).isTrue();
    }

    @Test
    void postsEventWhenLeaderChanges() {
        when(lockService.lock(any(), isNull())).thenReturn(Optional.of(mock(Lock.class)));

        leaderElectionService.startAsync().awaitRunning();

        verify(eventBus, timeout(10_000)).post(any(LeaderChangedEvent.class));
        assertThat(leaderElectionService.isLeader()).isTrue();

        when(lockService.lock(any(), isNull())).thenReturn(Optional.empty());
        verify(eventBus, timeout(10_000).times(2)).post(any(LeaderChangedEvent.class));
        assertThat(leaderElectionService.isLeader()).isFalse();
    }

    @Test
    void pausesPollingAfterDowngradeFromLeader() {
        final AtomicInteger lockInvocations = new AtomicInteger();
        final AtomicReference<Lock> lock = new AtomicReference<>();

        when(lockService.lock(any(), isNull())).then(i -> {
            lockInvocations.incrementAndGet();
            return Optional.ofNullable(lock.get());
        });

        leaderElectionService.startAsync().awaitRunning();
        lock.set(mock(Lock.class));
        await().until(() -> leaderElectionService.isLeader());

        // polling should continue
        int lockCount = lockInvocations.get();
        Uninterruptibles.sleepUninterruptibly(configuration.getLeaderElectionLockPollingInterval().multipliedBy(2));
        assertThat(lockInvocations.get()).isGreaterThan(lockCount);

        lock.set(null);
        await().until(() -> !leaderElectionService.isLeader());

        // polling should have been paused
        lockCount = lockInvocations.get();
        Uninterruptibles.sleepUninterruptibly(configuration.getLeaderElectionLockPollingInterval().multipliedBy(2));
        assertThat(lockInvocations.get()).isEqualTo(lockCount);
    }

    @Test
    void handlesConsistentFailure() {
        Lock lock = mock(Lock.class);
        when(lockService.lock(any(), isNull()))
                .thenReturn(Optional.of(lock))
                .thenThrow(new RuntimeException("ouch"));

        leaderElectionService.startAsync().awaitRunning();
        verify(eventBus, timeout(10_000).times(2)).post(any(LeaderChangedEvent.class));
        assertThat(leaderElectionService.isLeader()).isFalse();
    }

    @Test
    void doesNotTerminateOnExceptionInMainLoop() {
        doThrow(new RuntimeException("ouch")).when(eventBus).post(any(LeaderChangedEvent.class));
        when(lockService.lock(any(), isNull())).thenReturn(Optional.of(mock(Lock.class)));
        leaderElectionService.startAsync().awaitRunning();

        // if the main loop would not handle the exception, we wouldn't last two iterations
        verify(lockService, timeout(10_000).atLeast(2)).lock(any(), isNull());
        verify(eventBus).post(any(LeaderChangedEvent.class));
    }
}
