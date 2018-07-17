/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.periodical;

import com.google.common.collect.Lists;
import org.graylog2.plugin.periodical.Periodical;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeriodicalsTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ScheduledExecutorService scheduler;
    @Mock
    private ScheduledExecutorService daemonScheduler;
    @Mock
    private Periodical periodical;
    private ScheduledFuture<Object> future;
    private Periodicals periodicals;

    @Before
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

        assertFalse("Periodical should not be in the futures Map", periodicals.getFutures().containsKey(periodical));
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

        assertTrue("Periodical was not added to the futures Map", periodicals.getFutures().containsKey(periodical));
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

        assertEquals("Future for the periodical was not added to the futures Map", future, periodicals.getFutures().get(periodical));
    }

    @Test
    public void testGetAll() throws Exception {
        periodicals.registerAndStart(periodical);

        assertEquals("getAll() did not return all periodicals", Lists.newArrayList(periodical), periodicals.getAll());
    }

    @Test
    public void testGetAllReturnsACopyOfThePeriodicalsList() throws Exception {
        periodicals.registerAndStart(periodical);

        periodicals.getAll().add(periodical);

        assertEquals("getAll() did not return a copy of the periodicals List", 1, periodicals.getAll().size());
    }

    @Test
    public void testGetAllStoppedOnGracefulShutdown() throws Exception {
        final Periodical periodical2 = mock(Periodical.class);
        when(periodical2.stopOnGracefulShutdown()).thenReturn(true);

        periodicals.registerAndStart(periodical);
        periodicals.registerAndStart(periodical2);

        List<Periodical> allStoppedOnGracefulShutdown = periodicals.getAllStoppedOnGracefulShutdown();

        assertFalse("periodical without graceful shutdown is in the list", allStoppedOnGracefulShutdown.contains(periodical));
        assertTrue("graceful shutdown periodical is not in the list", allStoppedOnGracefulShutdown.contains(periodical2));
        assertEquals("more graceful shutdown periodicals in the list", 1, allStoppedOnGracefulShutdown.size());
    }

    @Test
    public void testGetFutures() throws Exception {
        periodicals.registerAndStart(periodical);

        assertTrue("missing periodical in future Map", periodicals.getFutures().containsKey(periodical));
        assertEquals(1, periodicals.getFutures().size());
    }

    @Test
    public void testGetFuturesReturnsACopyOfTheMap() throws Exception {
        final Periodical periodical2 = mock(Periodical.class);

        periodicals.registerAndStart(periodical);

        periodicals.getFutures().put(periodical2, null);

        assertFalse("getFutures() did not return a copy of the Map", periodicals.getFutures().containsKey(periodical2));
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
            public boolean masterOnly() {
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
