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
package org.graylog2.streams;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.name.Named;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Routes a {@link org.graylog2.plugin.Message} to its streams.
 */
public class StreamRouter {
    private static final Logger LOG = LoggerFactory.getLogger(StreamRouter.class);

    private static final long ENGINE_UPDATE_INTERVAL = 1L;

    protected final StreamService streamService;
    private final ServerStatus serverStatus;

    private final AtomicReference<StreamRouterEngine> routerEngine = new AtomicReference<>(null);

    @Inject
    public StreamRouter(StreamService streamService,
                        ServerStatus serverStatus,
                        StreamRouterEngine.Factory routerEngineFactory,
                        @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.streamService = streamService;
        this.serverStatus = serverStatus;

        final StreamRouterEngineUpdater streamRouterEngineUpdater = new StreamRouterEngineUpdater(routerEngine, routerEngineFactory, streamService, executorService());
        this.routerEngine.set(streamRouterEngineUpdater.getNewEngine());
        scheduler.scheduleAtFixedRate(streamRouterEngineUpdater, 0, ENGINE_UPDATE_INTERVAL, TimeUnit.SECONDS);
    }

    private ExecutorService executorService() {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("stream-router-%d")
                .setDaemon(true)
                .build();
        return Executors.newCachedThreadPool(threadFactory);
    }

    public List<Stream> route(final Message msg) {
        final StreamRouterEngine engine = routerEngine.get();

        msg.recordCounter(serverStatus, "streams-evaluated", engine.getStreams().size());

        return engine.match(msg);
    }

    private class StreamRouterEngineUpdater implements Runnable {
        private final AtomicReference<StreamRouterEngine> routerEngine;
        private final StreamRouterEngine.Factory engineFactory;
        private final StreamService streamService;
        private final ExecutorService executorService;

        public StreamRouterEngineUpdater(AtomicReference<StreamRouterEngine> routerEngine,
                                         StreamRouterEngine.Factory engineFactory,
                                         StreamService streamService,
                                         ExecutorService executorService) {
            this.routerEngine = routerEngine;
            this.engineFactory = engineFactory;
            this.streamService = streamService;
            this.executorService = executorService;
        }

        @Override
        public void run() {
            try {
                final StreamRouterEngine engine = getNewEngine();

                if (engine.getFingerprint().equals(routerEngine.get().getFingerprint())) {
                    LOG.debug("Not updating router engine, streams did not change (fingerprint={})", engine.getFingerprint());
                } else {
                    LOG.debug("Updating to new stream router engine. (old-fingerprint={} new-fingerprint={}",
                            routerEngine.get().getFingerprint(), engine.getFingerprint());
                    routerEngine.set(engine);
                }
            } catch (Exception e) {
                LOG.error("Stream router engine update failed!", e);
            }
        }

        private StreamRouterEngine getNewEngine() {
            return engineFactory.create(streamService.loadAllEnabled(), executorService);
        }
    }
}
