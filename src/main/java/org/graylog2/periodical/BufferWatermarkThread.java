/**
 * Copyright 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Gauge;
import org.graylog2.Core;
import org.graylog2.buffers.BufferWatermark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BufferWatermarkThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(BufferWatermarkThread.class);
    
    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 5;
    
    private final Core graylogServer;
    
    public BufferWatermarkThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void run() {
        checkValidity(graylogServer.processBufferWatermark());
        checkValidity(graylogServer.outputBufferWatermark());
        
        int ringSize = graylogServer.getConfiguration().getRingSize();
        
        final BufferWatermark oWm = new BufferWatermark(ringSize, graylogServer.outputBufferWatermark());
        final BufferWatermark pWm = new BufferWatermark(ringSize, graylogServer.processBufferWatermark());

        graylogServer.getServerValues().writeBufferWatermarks(oWm, pWm);
        
        graylogServer.getServerValues().writeMasterCacheSizes(
                graylogServer.getInputCache().size(),
                graylogServer.getOutputCache().size()
        );
        
        bufferMetrics(oWm, pWm);
        masterCacheMetrics(graylogServer.getInputCache().size(), graylogServer.getOutputCache().size());
    }
    
    private void checkValidity(AtomicInteger watermark) {
        // This should never happen, but just to make sure...
        int x = watermark.get();
        if (x < 0) {
            LOG.warn("Reset a watermark to 0 because it was <{}>", x);
            watermark.set(0);
        }
    }
    
    private void bufferMetrics(final BufferWatermark outputBuffer, final BufferWatermark processBuffer) {
        Metrics.newGauge(BufferWatermarkThread.class, "OutputBufferWatermark", new Gauge<Integer>() {
            @Override
            public Integer value() {
                return outputBuffer.getUtilization();
            }
        });
        
        Metrics.newGauge(BufferWatermarkThread.class, "OutputBufferWatermarkPercentage", new Gauge<Float>() {
            @Override
            public Float value() {
                return outputBuffer.getUtilizationPercentage();
            }
        });
        
        Metrics.newGauge(BufferWatermarkThread.class, "ProcessBufferWatermark", new Gauge<Integer>() {
            @Override
            public Integer value() {
                return processBuffer.getUtilization();
            }
        });
        
        Metrics.newGauge(BufferWatermarkThread.class, "ProcessBufferWatermarkPercentage", new Gauge<Float>() {
            @Override
            public Float value() {
                return processBuffer.getUtilizationPercentage();
            }
        });
    }
    
    private void masterCacheMetrics(final int inputCacheSize, final int outputCacheSize) {
        Metrics.newGauge(BufferWatermarkThread.class, "InputCacheSize", new Gauge<Integer>() {
            @Override
            public Integer value() {
                return inputCacheSize;
            }
        });
        
        Metrics.newGauge(BufferWatermarkThread.class, "OutputCacheSize", new Gauge<Integer>() {
            @Override
            public Integer value() {
                return outputCacheSize;
            }
        });
    }
    
}
