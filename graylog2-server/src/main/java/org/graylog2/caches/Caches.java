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
package org.graylog2.caches;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import com.google.inject.Inject;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.InputCache;
import org.graylog2.inputs.OutputCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Caches {
    private static final Logger LOG = LoggerFactory.getLogger(Caches.class);
    private static final long DEFAULT_MAX_WAIT = 30l;

    private final Cache inputCache;
    private final Cache outputCache;

    @Inject
    public Caches(final InputCache inputCache, final OutputCache outputCache) {
        this.inputCache = inputCache;
        this.outputCache = outputCache;
    }

    public void waitForEmptyCaches() {
        waitForEmptyCaches(DEFAULT_MAX_WAIT, TimeUnit.SECONDS);
    }

    public void waitForEmptyCaches(final long maxWait, final TimeUnit timeUnit) {
        LOG.info("Waiting until all caches are empty.");
        final Callable<Boolean> checkForEmptyCaches = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (inputCache.isEmpty() && outputCache.isEmpty()) {
                    return true;
                } else {
                    LOG.info("Waiting for caches to drain ({} imc/{} omc).", inputCache.size(), outputCache.size());
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
            retryer.call(checkForEmptyCaches);
        } catch (RetryException e) {
            LOG.info("Caches not empty after {} {}. Giving up.", maxWait, timeUnit.name().toLowerCase());
            return;
        } catch (ExecutionException e) {
            LOG.error("Error while waiting for empty caches.", e);
            return;
        }

        LOG.info("All caches are empty. Continuing.");
    }
}
