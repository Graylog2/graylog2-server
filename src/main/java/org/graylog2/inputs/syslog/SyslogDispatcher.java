/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.inputs.syslog;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.graylog2.ThreadPool;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogDispatcher extends SimpleChannelHandler {

    private static final Logger LOG = Logger.getLogger(SyslogDispatcher.class);
    protected final ExecutorService executor = new ThreadPool(SyslogDispatcher.class.getName(), 128, 15000*10);
    protected static final Meter receivedMessagesMeter = Metrics.newMeter(SyslogDispatcher.class, "ReceivedMessages", "messages", TimeUnit.SECONDS);

    private SyslogProcessor processor;

    public SyslogDispatcher(GraylogServer server) {
        this.processor = new SyslogProcessor(server);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    receivedMessagesMeter.mark();

                    InetSocketAddress remoteAddress = (InetSocketAddress) e.getRemoteAddress();

                    ChannelBuffer buffer = (ChannelBuffer) e.getMessage();

                    byte[] readable = new byte[buffer.readableBytes()];
                    buffer.toByteBuffer().get(readable, buffer.readerIndex(), buffer.readableBytes());

                    processor.messageReceived(new String(readable), remoteAddress.getAddress());
                } catch (Exception ex) {
                    LOG.warn("Could not handle syslog message: " + ex.getCause());
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.warn("Could not handle syslog message.", e.getCause());
    }

}
