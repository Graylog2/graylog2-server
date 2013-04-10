/**
 * Copyright 2011, 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import org.graylog2.plugin.Tools;
import org.bson.types.ObjectId;
import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.graylog2.plugin.MessageCounter;

/**
 * Singleton holding the number of received messages for streams,
 * sources and a total.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class MessageCounterImpl implements MessageCounter {

    private Counter total = new Counter();
    private NonBlockingHashMap<String, Counter> streams =new NonBlockingHashMap<String, Counter>();
    private NonBlockingHashMap<String, Counter> sources = new NonBlockingHashMap<String, Counter>();

    private AtomicInteger throughput = new AtomicInteger();
    private long highestThroughput = 0;

    public int getTotalCount() {
        return (int) this.total.get();
    }

    public Map<String, Integer> getStreamCounts() {
        
        HashMap<String, Integer> r = new HashMap<String, Integer>(this.streams.size());
        
        for (Entry<String, Counter> entry : this.streams.entrySet()) {
            r.put(entry.getKey(),(int) entry.getValue().get());
        }
        
        return r;
    }

    public Map<String, Integer> getSourceCounts() {
        HashMap<String, Integer> r = new HashMap<String, Integer>(this.sources.size());
        
        for (Entry<String, Counter> entry : this.sources.entrySet()) {
            r.put(entry.getKey(),(int) entry.getValue().get());
        }
        
        return r;
    }

    public int getThroughput() {
        return (int) this.throughput.get();
    }

    public int getHighestThroughput() {
        return (int) this.highestThroughput;
    }

    public void resetAllCounts() {
        this.resetTotal();
        this.resetStreamCounts();
        this.resetSourceCounts();
    }

    public void resetSourceCounts() {
        this.sources = new NonBlockingHashMap<String, Counter>(this.sources.size());
    }

    public void resetStreamCounts() {
        this.streams = new NonBlockingHashMap<String, Counter>(this.streams.size());
    }

    public void resetTotal() {
        this.total.set(0);
    }

    public void resetThroughput() {
        this.throughput.set( 0 );
    }

    /**
     * Increment total count by 1.
     */
    public void incrementTotal() {
        this.total.increment();
    }

    /**
     * Increment five second throughput by 1.
     */
    public void incrementThroughput() {
        countUpThroughput(1);
    }

    /**
     * Count up the total count.
     *
     * @param x The value to add on top of current total count.
     */
    public void countUpTotal(final int x) {
        this.total.add( x );
    }

    /**
     * Counts up the five second througput counter which is handled and reset by
     * the ServerValueWriterThread.
     *
     * @param x The value to add on top of five second throuput.
     */
    public void countUpThroughput(final int x) {
        int t = this.throughput.addAndGet(x);

        if (t > this.highestThroughput) {
            this.highestThroughput = t;
        }
    }

    /**
     * Increment stream count by 1.
     *
     * @param streamId The ID of the stream which count to increment.
     */
    public void incrementStream(final ObjectId streamId) {
        this.countUpStream(streamId, 1);
    }

    /**
     * Count up the count of a stream.
     *
     * @param streamId The ID of the stream which count to increment.
     * @param x The value to add on top of the current stream count.
     */
    public void countUpStream(final ObjectId streamId, final int x) {
        Counter c = this.streams.get(streamId);
        if (c != null) {
            c.add(x);
        } else {
            c = new Counter();
            Counter c1 = this.streams.putIfAbsent(streamId.toString(), c );
            
            if ( c1 != null ) c= c1;
            
            c.add(x);
        }
    }

    /**
     * Increment source count by 1.
     *
     * @param source The name of the source which count to increment.
     */
    public void incrementSource(final String source) {
        this.countUpSource(source, 1);
    }

    /**
     * Count up the count of a sourcet.
     *
     * @param source The name of the source which count to increment.
     * @param x The value to add on top of the current source count.
     */
    public  void countUpSource(String source, final int x) {
        source = Tools.encodeBase64(source);
        Counter c = this.sources.get(source);
        if (c != null) {
            c.add(x);
        } else {
            c = new Counter();
            Counter c1 = this.sources.putIfAbsent(source, c);
            if (c1 !=null ) c = c1;
            c.add(x);
        }
    }

}