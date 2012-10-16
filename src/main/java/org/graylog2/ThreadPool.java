/**
 * Copyright 2012 Nikolay Bryskin <devel.niks@gmail.com>
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

package org.graylog2;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

class NamedThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(0);

    public NamedThreadFactory(String name) {
        this.namePrefix = "pool-" + name + "-thread-";
    }

    public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
    }
};

class CountedBlockingQueue extends ArrayBlockingQueue<Runnable> {
    private final Counter sizeCounter;
    private final Meter rejectedMeter;

    public CountedBlockingQueue(String name, int queueSize, boolean fair) {
        super(queueSize, fair);
        sizeCounter = Metrics.newCounter(CountedBlockingQueue.class, "queue-size", name);
        rejectedMeter = Metrics.newMeter(CountedBlockingQueue.class, "rejected", name, "messages", TimeUnit.SECONDS);
    }

    public boolean offer(Runnable r) {
        boolean res = super.offer(r);
        if (res) {
            sizeCounter.inc();
        } else {
            rejectedMeter.mark();
        }
        return res;
    }

    public Runnable take() throws InterruptedException {
        final Runnable r = super.take();
        sizeCounter.dec();
        return r;
    }
};

/**
 * @author Nikolay Bryskin <devel.niks@gmail.com>
 */
public class ThreadPool extends ThreadPoolExecutor {
    public ThreadPool(String name, int workerCount, int queueSize) {
        super(workerCount, workerCount, 30L, TimeUnit.SECONDS, new CountedBlockingQueue(name, queueSize, true), new NamedThreadFactory(name));
    }
}
