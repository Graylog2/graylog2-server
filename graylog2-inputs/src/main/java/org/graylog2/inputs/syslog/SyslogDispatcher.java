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
package org.graylog2.inputs.syslog;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static com.codahale.metrics.MetricRegistry.name;

public class SyslogDispatcher extends SimpleChannelHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(SyslogDispatcher.class);

    private SyslogProcessor processor;
    private final Meter receivedMessages;

    public SyslogDispatcher(MetricRegistry metricRegistry,
                            Buffer processBuffer,
                            Configuration config,
                            MessageInput sourceInput) {
        this.processor = new SyslogProcessor(metricRegistry, processBuffer, config, sourceInput);
        this.receivedMessages = metricRegistry.meter(name(sourceInput.getUniqueReadableId(), "receivedMessages"));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        processMessage(e);
    }

    void processMessage(MessageEvent e) throws BufferOutOfCapacityException {
        receivedMessages.mark();

        InetSocketAddress remoteAddress = (InetSocketAddress) e.getRemoteAddress();

        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();

        byte[] readable = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(readable, buffer.readerIndex(), buffer.readableBytes());

        this.processor.messageReceived(new String(readable, StandardCharsets.UTF_8), remoteAddress.getAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.debug("Could not handle syslog message.", e.getCause());

        if (ctx.getChannel() != null && !(ctx.getChannel() instanceof DatagramChannel)) {
            ctx.getChannel().close();
        }
    }

}
