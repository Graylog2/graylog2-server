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
package org.graylog2.inputs.transports;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GELFHttpHandlerTest {
    @Mock private MessageInput messageInput;
    @Mock private MetricRegistry metricRegistry;
    @Mock private ChannelHandlerContext ctx;
    @Mock private MessageEvent evt;
    @Mock private Channel channel;
    @Mock private HttpRequest request;
    @Mock private HttpHeaders headers;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Meter meter = mock(Meter.class);
        when(metricRegistry.meter(anyString())).thenReturn(meter);

        ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer("{}", Charset.defaultCharset());

        when(headers.get(HttpHeaders.Names.CONNECTION)).thenReturn(HttpHeaders.Values.CLOSE);

        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.headers()).thenReturn(headers);
        when(request.getProtocolVersion()).thenReturn(HttpVersion.HTTP_1_1);
        when(request.getContent()).thenReturn(channelBuffer);
        when(request.getUri()).thenReturn("/gelf");

        when(ctx.getChannel()).thenReturn(mock(Channel.class));

        ChannelFuture channelFuture = mock(ChannelFuture.class);

        when(channel.write(any())).thenReturn(channelFuture);

        when(evt.getMessage()).thenReturn(request);
        when(evt.getChannel()).thenReturn(channel);
    }

    @Test
    public void testBasicMessageReceived() throws Exception {
        final HttpTransport.Handler handler = new HttpTransport.Handler(true);

        handler.messageReceived(ctx, evt);

        verify(channel).write(any(HttpResponse.class));
        verify(ctx, atMost(1)).sendUpstream(any(ChannelEvent.class));
    }

    @Test
    public void testWithKeepalive() throws Exception {
        when(headers.get(HttpHeaders.Names.CONNECTION)).thenReturn(HttpHeaders.Values.KEEP_ALIVE);
        HttpTransport.Handler handler = new HttpTransport.Handler(true);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, atMost(1)).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.CONNECTION), HttpHeaders.Values.KEEP_ALIVE);
    }

    @Test
    public void testWithoutKeepalive() throws Exception {
        when(headers.get(HttpHeaders.Names.CONNECTION)).thenReturn(HttpHeaders.Values.CLOSE);
        HttpTransport.Handler handler = new HttpTransport.Handler(true);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, atMost(1)).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.CONNECTION), HttpHeaders.Values.CLOSE);
    }

    @Test
    public void testNotReactToNonPost() throws Exception {
        HttpTransport.Handler handler = new HttpTransport.Handler(true);
        when(request.getMethod()).thenReturn(HttpMethod.GET);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, never()).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.getStatus(), HttpResponseStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    public void testNotReactToOtherPath() throws Exception {
        HttpTransport.Handler handler = new HttpTransport.Handler(true);
        when(request.getUri()).thenReturn("/notgelf");

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, never()).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.getStatus(), HttpResponseStatus.NOT_FOUND);
    }

    @Test
    public void testNoCorsHeadersWithoutOrigin() throws Exception {
        HttpTransport.Handler handler = new HttpTransport.Handler(true);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, atMost(1)).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    public void testAddCorsHeadersForOrigin() throws Exception {
        HttpTransport.Handler handler = new HttpTransport.Handler(true);
        String origin = "localhost";
        when(this.headers.get(HttpHeaders.Names.ORIGIN)).thenReturn(origin);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, atMost(1)).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN), origin);
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS), "true");
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS), "Authorization");
    }

    @Test
    public void testAddCorsHeadersForDifferentOrigin() throws Exception {
        HttpTransport.Handler handler = new HttpTransport.Handler(true);
        String origin = "www.google.com";
        when(this.headers.get(HttpHeaders.Names.ORIGIN)).thenReturn(origin);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, atMost(1)).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN), origin);
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS), "true");
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS), "Authorization");
    }

    @Test
    public void testNoCorsHeadersForOriginIfDisabled() throws Exception {
        HttpTransport.Handler handler = new HttpTransport.Handler(false);
        String origin = "localhost";
        when(this.headers.get(HttpHeaders.Names.ORIGIN)).thenReturn(origin);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(ctx, atMost(1)).sendUpstream(any(ChannelEvent.class));

        HttpResponse response = argument.getValue();
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS));
    }
}
