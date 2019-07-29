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

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Singleton
public class JobSchedulerService extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(JobSchedulerService.class);

    private final JobExecutionEngine jobExecutionEngine;
    private final JobSchedulerConfig schedulerConfig;
    private final JobSchedulerClock clock;
    private final JobWorkerPool workerPool;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @Inject
    public JobSchedulerService(JobExecutionEngine.Factory engineFactory,
                               JobWorkerPool.Factory workerPoolFactory,
                               JobSchedulerConfig schedulerConfig,
                               JobSchedulerClock clock) {
        this.workerPool = workerPoolFactory.create("system", schedulerConfig.numberOfWorkerThreads());
        this.jobExecutionEngine = engineFactory.create(workerPool);
        this.schedulerConfig = schedulerConfig;
        this.clock = clock;
    }

    @Override
    protected void run() throws Exception {
        if (schedulerConfig.canRun()) {
            while (isRunning()) {
                LOG.debug("Starting scheduler loop iteration");
                try {
                    if (!jobExecutionEngine.execute() && isRunning()) {
                        // We wait because there are either no free worker threads or no runnable triggers
                        clock.sleepUninterruptibly(1, TimeUnit.SECONDS); // TODO: Duration should be configurable
                    }
                } catch (Exception e) {
                    LOG.error("Error running job execution engine", e);
                }
                LOG.debug("Ending scheduler loop iteration");
            }
        } else {
            LOG.debug("Scheduler cannot run on this node, waiting for shutdown");
            shutdownLatch.await();
        }
    }

    @Override
    protected void triggerShutdown() {
        shutdownLatch.countDown();
        jobExecutionEngine.shutdown();
    }
}
