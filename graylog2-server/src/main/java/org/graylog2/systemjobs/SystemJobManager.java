/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.systemjobs;

import com.eaio.uuid.UUID;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.Core;
import org.graylog2.activities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Core server;

    private static final int THREAD_POOL_SIZE = 15;

    private ExecutorService executor = Executors.newFixedThreadPool(
            THREAD_POOL_SIZE,
            new ThreadFactoryBuilder().setNameFormat("systemjob-executor-%d").build()
    );

    private final Map<String, SystemJob> jobs;

    public SystemJobManager(Core server) {
        this.server = server;

        jobs = new ConcurrentHashMap<String, SystemJob>();
    }

    public String submit(final SystemJob job) {
        final String jobClass = job.getClass().getCanonicalName();

        job.setId(new UUID().toString());
        jobs.put(job.getId(), job);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                Stopwatch x = new Stopwatch().start();
                job.execute();  // ... blocks until it finishes.
                x.stop();

                jobs.remove(job.getId());

                String msg = "SystemJob <" + job.getId() + "> [" + jobClass + "] finished in " + x.elapsed(TimeUnit.MILLISECONDS) + "ms.";
                LOG.info(msg);
                server.getActivityWriter().write(new Activity(msg, SystemJobManager.class));
            }
        });

        LOG.info("Submitted SystemJob <{}> [{}]", job.getId(), jobClass);
        return job.getId();
    }

}
