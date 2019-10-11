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
