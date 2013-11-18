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
package org.graylog2.inputs;

import com.google.common.collect.Queues;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.cliffc.high_scale_lib.Counter;
import org.graylog2.plugin.Message;

/**
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BasicCache implements Cache {
    
    private final ConcurrentLinkedQueue<Message> q;
    private final Counter counter;
    
    public BasicCache() {
        q = Queues.newConcurrentLinkedQueue();
        counter = new Counter();
    }
    
    @Override
    public void add(Message m) {
        q.add(m);
        counter.increment();
    }
    
    @Override
    public Message pop() {
        Message m = q.poll();
        
        // Only decrement counter if we really popped something.
        if (m != null) {
            counter.decrement();
            return m;
        } else {
            return null;
        }
    }
    
    @Override
    public int size() {
        return counter.intValue();
    }

    @Override
    public void clear() {
        q.clear();
    }

}
