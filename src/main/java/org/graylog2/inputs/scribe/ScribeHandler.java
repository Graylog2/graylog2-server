/**
 * Copyright 2011 Rackspace Hosting Inc.
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

package org.graylog2.inputs.scribe;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.graylog2.GraylogServer;
import scribe.LogEntry;
import scribe.ResultCode;
import scribe.Scribe.Iface;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScribeHandler.java:
 *
 * Object responsible for handling a Scribe request containing a list of GELF entries
 *
 */
public class ScribeHandler implements Iface {
    private final GraylogServer graylogServer;
    private final ExecutorService executor;
    private final AtomicInteger outstanding;
    private static final Logger LOG = Logger.getLogger(ScribeHandler.class);

    private static int THREADS_POOL_SIZE = 5;
    private static int MAX_OUTSTANDING = 10; // batches


    public ScribeHandler(GraylogServer server) {
        this.graylogServer = server;
        this.outstanding = new AtomicInteger(0);
        this.executor = Executors.newFixedThreadPool(THREADS_POOL_SIZE);
    }

    @Override
    public ResultCode Log(List<LogEntry> messages) throws TException {
        LOG.info("Received " + messages.size() + " messages.");
        if (outstanding.get() > MAX_OUTSTANDING) {
            LOG.info("TRY_LATER: Hit maximum capacity. Number of outstanding batches: " + outstanding.get());
            return ResultCode.TRY_LATER;
        }

        outstanding.incrementAndGet();
        executor.submit(new ScribeBatchHandler(graylogServer, messages, outstanding));

        return ResultCode.OK;
    }
}
