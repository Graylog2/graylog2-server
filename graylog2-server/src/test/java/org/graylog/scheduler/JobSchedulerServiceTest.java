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
package org.graylog.scheduler;

import org.graylog.scheduler.JobSchedulerService.InterruptibleSleeper;
import org.junit.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobSchedulerServiceTest {
    @Test
    public void interruptibleSleeper() throws Exception {
        final Semaphore semaphore = spy(new Semaphore(1));
        final InterruptibleSleeper sleeper = new InterruptibleSleeper(semaphore);

        when(semaphore.tryAcquire(1, TimeUnit.SECONDS)).thenReturn(false);
        assertThat(sleeper.sleep(1, TimeUnit.SECONDS)).isTrue();
        verify(semaphore, times(1)).drainPermits();
        verify(semaphore, times(1)).tryAcquire(1, TimeUnit.SECONDS);

        reset(semaphore);

        when(semaphore.tryAcquire(1, TimeUnit.SECONDS)).thenReturn(true);
        assertThat(sleeper.sleep(1, TimeUnit.SECONDS)).isFalse();
        verify(semaphore, times(1)).drainPermits();
        verify(semaphore, times(1)).tryAcquire(1, TimeUnit.SECONDS);

        reset(semaphore);

        sleeper.interrupt();
        verify(semaphore, times(1)).release();
    }
}
