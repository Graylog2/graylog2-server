package org.graylog.scheduler;

import org.graylog.scheduler.JobSchedulerService.InterruptibleSleeper;
import org.junit.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobSchedulerServiceTest {
    @Test
    public void interruptibleSleeper() throws Exception {
        assertThatCode(() -> new InterruptibleSleeper(new Semaphore(0)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new InterruptibleSleeper(new Semaphore(2)))
            .isInstanceOf(IllegalArgumentException.class);

        final Semaphore semaphore = spy(new Semaphore(1));
        final InterruptibleSleeper sleeper = new InterruptibleSleeper(semaphore);

        when(semaphore.tryAcquire(1, TimeUnit.SECONDS)).thenReturn(false);
        assertThat(sleeper.sleep(1, TimeUnit.SECONDS)).isTrue();
        verify(semaphore, times(1)).tryAcquire();
        verify(semaphore, times(1)).tryAcquire(1, TimeUnit.SECONDS);
        verify(semaphore, times(1)).release();

        reset(semaphore);

        when(semaphore.tryAcquire(1, TimeUnit.SECONDS)).thenReturn(true);
        assertThat(sleeper.sleep(1, TimeUnit.SECONDS)).isFalse();
        verify(semaphore, times(1)).tryAcquire();
        verify(semaphore, times(1)).tryAcquire(1, TimeUnit.SECONDS);
        verify(semaphore, times(1)).release();

        reset(semaphore);

        sleeper.interrupt();
        verify(semaphore, times(1)).release();
    }
}
