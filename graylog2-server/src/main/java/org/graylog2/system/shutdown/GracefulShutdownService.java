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
package org.graylog2.system.shutdown;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A service that participates in the Graylog server graceful shutdown.
 * <p>
 * Services can implement {@link GracefulShutdownHook} and register themselves with this service to make sure they
 * get shut down properly on server shutdown.
 * <p>
 * Make sure to use {@link #unregister(GracefulShutdownHook)} if a registered service is shutting down before the
 * server shutdown to avoid leaking service instances in the {@link GracefulShutdownService}.
 *
 * See {@link GracefulShutdownHook} for an example.
 */
@Singleton
public class GracefulShutdownService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdownService.class);

    private final Set<GracefulShutdownHook> shutdownHooks = ConcurrentHashMap.newKeySet();

    @Override
    protected void startUp() throws Exception {
        // Nothing to do
    }

    @Override
    protected void shutDown() throws Exception {
        if (shutdownHooks.isEmpty()) {
            return;
        }

        // Make sure we don't run this in parallel
        synchronized (shutdownHooks) {
            try {
                // Use an executor to run the shutdown hooks in parallel but don't start too many threads
                // TODO: Make max number of threads user configurable
                final ExecutorService executor = executorService(Math.min(shutdownHooks.size(), 10));
                final CountDownLatch latch = new CountDownLatch(shutdownHooks.size());

                LOG.info("Running graceful shutdown for <{}> shutdown hooks", shutdownHooks.size());
                for (final GracefulShutdownHook shutdownHook : shutdownHooks) {
                    executor.submit(() -> {
                        try {
                            shutdownHook.doGracefulShutdown();
                        } catch (Exception e) {
                            LOG.error("Problem shutting down <{}>", shutdownHook, e);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (Exception e) {
                LOG.error("Problem shutting down registered hooks", e);
            }
        }
    }

    /**
     * Register a shutdown hook with the service.
     * @param shutdownHook a class that implements {@link GracefulShutdownHook}
     */
    public void register(GracefulShutdownHook shutdownHook) {
        shutdownHooks.add(shutdownHook);
    }

    /**
     * Remove a previously registered shutdown hook from the service.
     * <p>
     * This needs to be called if a registered service will be stopped before the server shuts down.
     * @param shutdownHook a class that implements {@link GracefulShutdownHook}
     */
    public void unregister(GracefulShutdownHook shutdownHook) {
        shutdownHooks.remove(shutdownHook);
    }

    private ExecutorService executorService(final int maxThreads) {
        return new ThreadPoolExecutor(0,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadFactoryBuilder()
                        .setNameFormat("graceful-shutdown-service-%d")
                        .build());
    }
}
