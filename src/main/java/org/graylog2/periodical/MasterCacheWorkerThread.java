/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.periodical;

import org.graylog2.Core;
import org.graylog2.buffers.Cache;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MasterCacheWorkerThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MasterCacheWorkerThread.class);

    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 1;

    private final Cache cache;
    private final String cacheName;
    private final Buffer targetBuffer;

    public MasterCacheWorkerThread(Core graylogServer, Cache cache, Buffer targetBuffer) {
        this.cache = cache;
        this.cacheName = cache.getClass().getCanonicalName();
        
        this.targetBuffer = targetBuffer;
    }

    @Override
    public void run() {
        while(true) {
            try {
                if (cache.size() > 0) {
                    LOG.debug("{} contains {} messages. Trying to process them.", cacheName, cache.size());
                    
                    while (true) {
                        if (cache.size() <= 0) {
                            LOG.debug("Read all messages from {}.", cacheName);
                            break;
                        }
                        
                        if (targetBuffer.hasCapacity()) {
                            try {
                                LOG.debug("Reading message from {}.", cacheName);
                                targetBuffer.insertFailFast(cache.pop());
                            } catch (BufferOutOfCapacityException ex) {
                                LOG.debug("Target buffer out of capacity in {}. Breaking.", cacheName);
                                break;
                            }
                        }
                    }
                }
            } catch(Exception e) {
                LOG.error("Error while trying to work on MasterCache <{}>.", cacheName, e);
                try {
                    Thread.sleep(1*1000);
                } catch(InterruptedException ex) { /* */ }
            }
            
            try {
                Thread.sleep(100);
            } catch(InterruptedException ex) { /* */ }
        }
    }
    
}
