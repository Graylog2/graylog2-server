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
package org.graylog2.buffers;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Buffers {
    private static final Logger LOG = LoggerFactory.getLogger(Buffers.class);
    private static final long DEFAULT_MAX_WAIT = 30l;

    private final ProcessBuffer processBuffer;
    private final OutputBuffer outputBuffer;

    @Inject
    public Buffers(final ProcessBuffer processBuffer, final OutputBuffer outputBuffer) {
        this.processBuffer = processBuffer;
        this.outputBuffer = outputBuffer;
    }

    public void waitForEmptyBuffers() {
        waitForEmptyBuffers(DEFAULT_MAX_WAIT, TimeUnit.SECONDS);
    }

    public void waitForEmptyBuffers(final long maxWait, final TimeUnit timeUnit) {
        LOG.info("Waiting until all buffers are empty.");
        final Callable<Boolean> checkForEmptyBuffers = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (processBuffer.isEmpty() && outputBuffer.isEmpty()) {
                    return true;
                } else {
                    LOG.info("Waiting for buffers to drain. ({}p/{}o)", processBuffer.getUsage(), outputBuffer.getUsage());
                }

                return false;
            }
        };

        final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.not(Predicates.equalTo(Boolean.TRUE)))
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterDelay(timeUnit.toMillis(maxWait)))
                .build();

        try {
            retryer.call(checkForEmptyBuffers);
        } catch (RetryException e) {
            LOG.info("Buffers not empty after {} {}. Giving up.", maxWait, timeUnit.name().toLowerCase());
            return;
        } catch (ExecutionException e) {
            LOG.error("Error while waiting for empty buffers.", e);
            return;
        }

        LOG.info("All buffers are empty. Continuing.");
    }
}
