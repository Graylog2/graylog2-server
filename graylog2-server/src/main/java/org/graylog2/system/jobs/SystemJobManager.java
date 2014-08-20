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
package org.graylog2.system.jobs;

import com.eaio.uuid.UUID;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJobManager {

    private static final Logger LOG = LoggerFactory.getLogger(SystemJobManager.class);

    private static final int THREAD_POOL_SIZE = 15;
    private final ActivityWriter activityWriter;

    private ExecutorService executor = Executors.newFixedThreadPool(
            THREAD_POOL_SIZE,
            new ThreadFactoryBuilder().setNameFormat("systemjob-executor-%d").build()
    );

    private final Map<String, SystemJob> jobs;

    @Inject
    public SystemJobManager(ActivityWriter activityWriter) {
        this.activityWriter = activityWriter;
        jobs = new ConcurrentHashMap<>();
    }

    public String submit(final SystemJob job) throws SystemJobConcurrencyException {
        int concurrent = concurrentJobs(job.getClass());

        if (concurrent >= job.maxConcurrency()) {
            throw new SystemJobConcurrencyException("The maximum of parallel [" + job.getClass().getCanonicalName().toString() + "] is locked " +
                    "to <" + job.maxConcurrency() + "> but <" + concurrent + "> are running.");
        }

        final String jobClass = job.getClass().getCanonicalName();

        job.setId(new UUID().toString());
        jobs.put(job.getId(), job);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                job.markStarted();

                Stopwatch x = Stopwatch.createStarted();

                job.execute();  // ... blocks until it finishes.
                x.stop();

                jobs.remove(job.getId());

                String msg = "SystemJob <" + job.getId() + "> [" + jobClass + "] finished in " + x.elapsed(TimeUnit.MILLISECONDS) + "ms.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, SystemJobManager.class));
            }
        });

        LOG.info("Submitted SystemJob <{}> [{}]", job.getId(), jobClass);
        return job.getId();
    }

    public Map<String, SystemJob> getRunningJobs() {
        return jobs;
    }

    public int concurrentJobs(Class jobClass) {
        int concurrent = 0;

        for (SystemJob job : jobs.values()) {
            if (job.getClass().equals(jobClass)) {
                concurrent += 1;
            }
        }

        return concurrent;
    }

}
