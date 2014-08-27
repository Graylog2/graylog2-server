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
package org.graylog2.inputs;

import com.google.common.collect.Queues;
import org.cliffc.high_scale_lib.Counter;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.actors.threadpool.Arrays;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BasicCache implements Cache, InputCache, OutputCache {
    private static final Logger LOG = LoggerFactory.getLogger(BasicCache.class);

    //private final ConcurrentLinkedQueue<Message> q;
    private final LinkedBlockingQueue<Message> q;

    public BasicCache() {
        //q = Queues.newConcurrentLinkedQueue();
        q = Queues.newLinkedBlockingQueue();
    }
    
    @Override
    public void add(Message m) {
        q.add(m);
    }

    @Override
    public void add(Collection<Message> m) {
        q.addAll(m);
    }

    @Override
    public Message pop() {
        try {
            return q.take();
        } catch (InterruptedException e) {
            LOG.error("Interrupted during wait for new element: ", e);
        }
        return null;
    }

    @Override
    public int drainTo(Collection<? super Message> c, int limit) {
        return q.drainTo(c, limit);
    }

    @Override
    public int size() {
        return q.size();
    }

    @Override
    public boolean isEmpty() {
        return q.isEmpty();
    }

}
