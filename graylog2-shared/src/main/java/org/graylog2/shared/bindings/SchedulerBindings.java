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
package org.graylog2.shared.bindings;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.graylog2.periodical.Periodicals;
import org.graylog2.plugin.Tools;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SchedulerBindings extends AbstractModule {
    private static final int SCHEDULED_THREADS_POOL_SIZE = 30;

    @Override
    protected void configure() {
        // TODO Add instrumentation to ExecutorService and ThreadFactory
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-%d")
                        .setDaemon(false)
                        .setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LoggerFactory.getLogger("org.graylog2.scheduler.Scheduler")))
                        .build()
        );

        bind(ScheduledExecutorService.class).annotatedWith(Names.named("scheduler")).toInstance(scheduler);

        // TODO Add instrumentation to ExecutorService and ThreadFactory
        final ScheduledExecutorService daemonScheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-daemon-%d")
                        .setDaemon(true)
                        .setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LoggerFactory.getLogger("org.graylog2.scheduler.DaemonScheduler")))
                        .build()
        );

        bind(ScheduledExecutorService.class).annotatedWith(Names.named("daemonScheduler")).toInstance(daemonScheduler);
        bind(Periodicals.class).toInstance(new Periodicals(scheduler, daemonScheduler));
    }
}
