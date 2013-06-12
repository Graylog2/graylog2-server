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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

    public SystemJobManager(Core server) {
        this.server = server;
    }

    public void submit(SystemJob job) {
        job.setJobReference(new SystemJobReference());
        executor.submit((Runnable) job);
        LOG.info("Submitted SystemJob <{}>", job.getClass().getCanonicalName());
    }

}
