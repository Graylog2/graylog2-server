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
package org.graylog2.cluster.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RefreshingLockServiceTest {

    @Mock
    private LockService lockService;

    @Mock
    private ScheduledExecutorService scheduler;

    private RefreshingLockService refreshingLockService;
    
    private final Lock firstLock = Lock.builder()
            .resource("first-resource")
            .lockedBy("node-123")
            .createdAt(ZonedDateTime.now(ZoneOffset.UTC))
            .updatedAt(ZonedDateTime.now(ZoneOffset.UTC))
            .build();

    private final Lock secondLock = Lock.builder()
            .resource("second-resource")
            .lockedBy("node-123")
            .createdAt(ZonedDateTime.now(ZoneOffset.UTC))
            .updatedAt(ZonedDateTime.now(ZoneOffset.UTC))
            .build();

    @BeforeEach
    void setUp() {
        refreshingLockService = new RefreshingLockService(
                lockService,
                scheduler,
                Duration.ofMinutes(5)
        );
    }

    @Test
    void throwsIllegalStateExceptionWhenAcquiringLockWhileAlreadyHoldingOne() throws AlreadyLockedException {
        // Mock the lock service to return locks
        when(lockService.lock(eq("first-resource"), anyString()))
                .thenReturn(Optional.of(firstLock));
        when(lockService.lock(eq("second-resource"), anyString()))
                .thenReturn(Optional.of(secondLock));

        // Acquire first lock successfully
        refreshingLockService.acquireAndKeepLock("first-resource", "context-1");

        // Attempt to acquire second lock while still holding the first
        assertThatThrownBy(() -> refreshingLockService.acquireAndKeepLock("second-resource", "context-2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to acquire new lock, already holding lock that would get lost");
    }

    @Test
    void throwsIllegalStateExceptionWhenAcquiringLockWithMaxConcurrencyWhileAlreadyHoldingOne() throws AlreadyLockedException {
        // Mock the lock service to return locks
        when(lockService.lock(eq("first-resource"), eq(1)))
                .thenReturn(Optional.of(firstLock));
        when(lockService.lock(eq("second-resource"), eq(1)))
                .thenReturn(Optional.of(secondLock));

        // Acquire first lock successfully
        refreshingLockService.acquireAndKeepLock("first-resource", 1);

        // Attempt to acquire second lock while still holding the first
        assertThatThrownBy(() -> refreshingLockService.acquireAndKeepLock("second-resource", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to acquire new lock, already holding lock that would get lost");
    }
}
