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
package org.graylog2.periodical;

import com.google.common.collect.Lists;
import org.graylog2.plugin.periodical.Periodical;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PeriodicalsTest {

    @Mock
    private ScheduledExecutorService scheduler;
    @Mock
    private ScheduledExecutorService daemonScheduler;
    @Mock
    private Periodical periodical;
    private ScheduledFuture<Object> future;
    private Periodicals periodicals;

    @BeforeEach
    public void setUp() throws Exception {
        periodicals = new Periodicals(scheduler, daemonScheduler);
        future = createScheduledFuture();

        when(scheduler.scheduleAtFixedRate(
                periodical,
                periodical.getInitialDelaySeconds(),
                periodical.getPeriodSeconds(),
                TimeUnit.SECONDS
        )).thenReturn((ScheduledFuture) future);
    }

    @Test
    public void testRegisterAndStartWithRunsForeverPeriodical() throws Exception {
        // Starts the periodical in a separate thread.
        when(periodical.runsForever()).thenReturn(true);

        periodicals.registerAndStart(periodical);

        verify(daemonScheduler, never()).scheduleAtFixedRate(
                periodical,
                periodical.getInitialDelaySeconds(),
                periodical.getPeriodSeconds(),
                TimeUnit.SECONDS
        );

        verify(scheduler, never()).scheduleAtFixedRate(
                periodical,
                periodical.getInitialDelaySeconds(),
                periodical.getPeriodSeconds(),
                TimeUnit.SECONDS
        );

        // TODO This does not work because the verify() runs before the run() method on the periodical has been called.
        //      Fixable by using an injectable ThreadFactoryBuilder so we can properly mock?
        //verify(periodical).run();

        assertFalse(periodicals.getFutures().containsKey(periodical), "Periodical should not be in the futures Map");
    }

    @Test
    public void testRegisterAndStartInvokeDaemonScheduler() throws Exception {
        // Uses the daemon scheduler for daemon periodicals.
        when(periodical.isDaemon()).thenReturn(true);
        when(periodical.runsForever()).thenReturn(false);

        periodicals.registerAndStart(periodical);

        verify(daemonScheduler).scheduleAtFixedRate(
                periodical,
                periodical.getInitialDelaySeconds(),
                periodical.getPeriodSeconds(),
                TimeUnit.SECONDS
        );

        verify(periodical, never()).run();

        assertTrue(periodicals.getFutures().containsKey(periodical), "Periodical was not added to the futures Map");
    }

    @Test
    public void testRegisterAndStartInvokeScheduler() throws Exception {
        // Uses the regular scheduler for non-daemon periodicals.
        when(periodical.isDaemon()).thenReturn(false);
        when(periodical.runsForever()).thenReturn(false);

        periodicals.registerAndStart(periodical);

        verify(scheduler).scheduleAtFixedRate(
                periodical,
                periodical.getInitialDelaySeconds(),
                periodical.getPeriodSeconds(),
                TimeUnit.SECONDS
        );

        verify(periodical, never()).run();

        assertEquals(future, periodicals.getFutures().get(periodical), "Future for the periodical was not added to the futures Map");
    }

    @Test
    public void testGetAll() throws Exception {
        periodicals.registerAndStart(periodical);

        assertEquals(Lists.newArrayList(periodical), periodicals.getAll(), "getAll() did not return all periodicals");
    }

    @Test
    public void testGetAllReturnsACopyOfThePeriodicalsList() throws Exception {
        periodicals.registerAndStart(periodical);

        periodicals.getAll().add(periodical);

        assertEquals(1, periodicals.getAll().size(), "getAll() did not return a copy of the periodicals List");
    }

    @Test
    public void testGetAllStoppedOnGracefulShutdown() throws Exception {
        final Periodical periodical2 = mock(Periodical.class);
        when(periodical2.stopOnGracefulShutdown()).thenReturn(true);

        periodicals.registerAndStart(periodical);
        periodicals.registerAndStart(periodical2);

        List<Periodical> allStoppedOnGracefulShutdown = periodicals.getAllStoppedOnGracefulShutdown();

        assertFalse(allStoppedOnGracefulShutdown.contains(periodical), "periodical without graceful shutdown is in the list");
        assertTrue(allStoppedOnGracefulShutdown.contains(periodical2), "graceful shutdown periodical is not in the list");
        assertEquals(1, allStoppedOnGracefulShutdown.size(), "more graceful shutdown periodicals in the list");
    }

    @Test
    public void testGetFutures() throws Exception {
        periodicals.registerAndStart(periodical);

        assertTrue(periodicals.getFutures().containsKey(periodical), "missing periodical in future Map");
        assertEquals(1, periodicals.getFutures().size());
    }

    @Test
    public void testGetFuturesReturnsACopyOfTheMap() throws Exception {
        final Periodical periodical2 = mock(Periodical.class);

        periodicals.registerAndStart(periodical);

        periodicals.getFutures().put(periodical2, null);

        assertFalse(periodicals.getFutures().containsKey(periodical2), "getFutures() did not return a copy of the Map");
        assertEquals(1, periodicals.getFutures().size());
    }

    @Test
    public void testExceptionIsNotUncaught() {

        final Logger logger = mock(Logger.class);
        final Periodical periodical1 = new Periodical() {
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
                return false;
            }

            @Override
            public int getInitialDelaySeconds() {
                return 0;
            }

            @Override
            public int getPeriodSeconds() {
                return 1;
            }

            @Override
            protected Logger getLogger() {
                return logger;
            }

            @Override
            public void doRun() {
                throw new NullPointerException();
            }
        };

        periodical1.run();
        // the uncaught exception from doRun should have been logged
        verify(logger, atLeastOnce()).error(anyString(), any(Throwable.class));
    }

    private ScheduledFuture<Object> createScheduledFuture() {
        return new ScheduledFuture<Object>() {
            @Override
            public long getDelay(TimeUnit unit) {
                return 0;
            }

            @Override
            public int compareTo(Delayed o) {
                return 0;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }
}
