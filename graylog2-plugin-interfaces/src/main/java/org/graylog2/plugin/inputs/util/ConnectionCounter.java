/**
 * Copyright (c) 2013 Lennart Koopmann <lennart@socketfeed.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graylog2.plugin.inputs.util;

import com.codahale.metrics.Gauge;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ConnectionCounter extends SimpleChannelHandler {

    private final AtomicInteger connections;
    private long totalConnections;

    public ConnectionCounter() {
        connections = new AtomicInteger();
        totalConnections = 0;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        connections.incrementAndGet();
        totalConnections++;
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        connections.decrementAndGet();
    }

    public int getConnectionCount() {
        return connections.get();
    }

    public long getTotalConnections() {
        return totalConnections;
    }

    public Gauge<Integer> gaugeCurrent() {
        return new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return getConnectionCount();
            }
        };
    }

    public Gauge<Long> gaugeTotal() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getTotalConnections();
            }
        };
    }

}
