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
package org.graylog2.inputs.syslog.tcp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class SyslogTCPFramingRouterHandlerTest {
    private ChannelUpstreamHandler handler;

    @Mock private ChannelHandlerContext context;
    @Mock private MessageEvent event;
    @Mock private ChannelPipeline pipeline;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        handler = new SyslogTCPFramingRouterHandler(2048, Delimiters.lineDelimiter());

        when(context.getPipeline()).thenReturn(pipeline);
        when(context.getName()).thenReturn("current");
    }

    @Test
    public void testMessageReceivedOctetFrame() throws Exception {
        final ChannelBuffer buf = ChannelBuffers.copiedBuffer("123 <45>", Charsets.UTF_8);

        when(event.getMessage()).thenReturn(buf);

        handler.handleUpstream(context, event);
        handler.handleUpstream(context, event);
        handler.handleUpstream(context, event);

        // Only add the decoder once!
        verify(pipeline, times(1)).addAfter(eq("current"), eq("framer-octet"), any(SyslogOctetCountFrameDecoder.class));
        verify(context, times(3)).sendUpstream(event);

        // Make sure the buffer does not get mutated.
        assertEquals(buf.toString(Charsets.UTF_8), "123 <45>");
    }

    @Test
    public void testMessageReceivedDelimiterFrame() throws Exception {
        final ChannelBuffer buf = ChannelBuffers.copiedBuffer("<45>", Charsets.UTF_8);

        when(event.getMessage()).thenReturn(buf);

        handler.handleUpstream(context, event);
        handler.handleUpstream(context, event);
        handler.handleUpstream(context, event);

        // Only add the decoder once!
        verify(pipeline, times(1)).addAfter(eq("current"), eq("framer-delimiter"), any(DelimiterBasedFrameDecoder.class));
        verify(context, times(3)).sendUpstream(event);

        // Make sure the buffer does not get mutated.
        assertEquals(buf.toString(Charsets.UTF_8), "<45>");
    }

    @Test
    public void testMessageReceivedWithEmptyBuffer() throws Exception {
        final ChannelBuffer buf = ChannelBuffers.copiedBuffer("", Charsets.UTF_8);

        when(event.getMessage()).thenReturn(buf);

        handler.handleUpstream(context, event);

        verify(pipeline, never()).addAfter(eq("current"), eq("framer-octet"), any(SyslogOctetCountFrameDecoder.class));
        verify(pipeline, never()).addAfter(eq("current"), eq("framer-delimiter"), any(DelimiterBasedFrameDecoder.class));
        verify(context, never()).sendUpstream(event);
    }
}