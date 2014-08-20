/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.periodical;

import com.google.common.collect.Lists;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class PeriodicalsTest {
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService daemonScheduler;
    private Periodicals periodicals;
    private ScheduledFuture<Object> future;
    private Periodical periodical;

    @BeforeMethod
    public void setUp() throws Exception {
        scheduler = mock(ScheduledExecutorService.class);
        daemonScheduler = mock(ScheduledExecutorService.class);
        periodicals = new Periodicals(scheduler, daemonScheduler);
        periodical = mock(Periodical.class);
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

        assertEquals(periodicals.getFutures().get(periodical), future, "Future for the periodical was not added to the futures Map");
    }

    @Test
    public void testGetAll() throws Exception {
        periodicals.registerAndStart(periodical);

        assertEquals(periodicals.getAll(), Lists.newArrayList(periodical), "getAll() did not return all periodicals");
    }

    @Test
    public void testGetAllReturnsACopyOfThePeriodicalsList() throws Exception {
        periodicals.registerAndStart(periodical);

        periodicals.getAll().add(periodical);

        assertEquals(periodicals.getAll().size(), 1, "getAll() did not return a copy of the periodicals List");
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
        assertEquals(allStoppedOnGracefulShutdown.size(), 1, "more graceful shutdown periodicals in the list");
    }

    @Test
    public void testGetFutures() throws Exception {
        periodicals.registerAndStart(periodical);

        assertTrue(periodicals.getFutures().containsKey(periodical), "missing periodical in future Map");
        assertEquals(periodicals.getFutures().size(), 1);
    }

    @Test
    public void testGetFuturesReturnsACopyOfTheMap() throws Exception {
        final Periodical periodical2 = mock(Periodical.class);

        periodicals.registerAndStart(periodical);

        periodicals.getFutures().put(periodical2, null);

        assertFalse(periodicals.getFutures().containsKey(periodical2), "getFutures() did not return a copy of the Map");
        assertEquals(periodicals.getFutures().size(), 1);
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

        try {
            periodical1.run();
            // the uncaught exception from doRun should have been logged
            verify(logger, atLeastOnce()).error(anyString(), any(Throwable.class));
        } catch (Exception e) {
            fail("run() should never propagate an unchecked exception!", e);
        }
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