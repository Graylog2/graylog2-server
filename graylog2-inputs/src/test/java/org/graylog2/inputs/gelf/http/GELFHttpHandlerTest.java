package org.graylog2.inputs.gelf.http;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.inputs.gelf.gelf.GELFMessage;
import org.graylog2.inputs.gelf.gelf.GELFProcessor;
import org.graylog2.plugin.inputs.MessageInput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GELFHttpHandlerTest {
    private GELFHttpHandler handler;
    @Mock private GELFProcessor gelfProcessor;
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

        ChannelFuture channelFuture = mock(ChannelFuture.class);

        when(channel.write(any())).thenReturn(channelFuture);

        when(evt.getMessage()).thenReturn(request);
        when(evt.getChannel()).thenReturn(channel);    }

    @Test
    public void testBasicMessageReceived() throws Exception {
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);

        handler.messageReceived(ctx, evt);

        verify(channel).write(any(HttpResponse.class));
        verify(this.gelfProcessor).messageReceived(any(GELFMessage.class), eq(this.messageInput));
    }

    @Test
    public void testWithKeepalive() throws Exception {
        when(headers.get(HttpHeaders.Names.CONNECTION)).thenReturn(HttpHeaders.Values.KEEP_ALIVE);
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor).messageReceived(any(GELFMessage.class), any(MessageInput.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.CONNECTION), HttpHeaders.Values.KEEP_ALIVE);
    }

    @Test
    public void testWithoutKeepalive() throws Exception {
        when(headers.get(HttpHeaders.Names.CONNECTION)).thenReturn(HttpHeaders.Values.CLOSE);
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor, times(1)).messageReceived(any(GELFMessage.class), any(MessageInput.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.CONNECTION), HttpHeaders.Values.CLOSE);
    }

    @Test
    public void testNotReactToNonPost() throws Exception {
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);
        when(request.getMethod()).thenReturn(HttpMethod.GET);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor, never()).messageReceived(any(GELFMessage.class), any(MessageInput.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.getStatus(), HttpResponseStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    public void testNotReactToOtherPath() throws Exception {
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);
        when(request.getUri()).thenReturn("/notgelf");

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor, never()).messageReceived(any(GELFMessage.class), any(MessageInput.class));

        HttpResponse response = argument.getValue();
        assertEquals(response.getStatus(), HttpResponseStatus.NOT_FOUND);
    }

    @Test
    public void testNoCorsHeadersWithoutOrigin() throws Exception {
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor).messageReceived(any(GELFMessage.class), eq(this.messageInput));

        HttpResponse response = argument.getValue();
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    public void testAddCorsHeadersForOrigin() throws Exception {
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);
        String origin = "localhost";
        when(this.headers.get(HttpHeaders.Names.ORIGIN)).thenReturn(origin);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor).messageReceived(any(GELFMessage.class), eq(this.messageInput));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN), origin);
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS), "true");
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS), "Authorization");
    }

    @Test
    public void testAddCorsHeadersForDifferentOrigin() throws Exception {
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, true);
        String origin = "www.google.com";
        when(this.headers.get(HttpHeaders.Names.ORIGIN)).thenReturn(origin);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor).messageReceived(any(GELFMessage.class), eq(this.messageInput));

        HttpResponse response = argument.getValue();
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN), origin);
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS), "true");
        assertEquals(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS), "Authorization");
    }

    @Test
    public void testNoCorsHeadersForOriginIfDisabled() throws Exception {
        this.handler = new GELFHttpHandler(metricRegistry, messageInput, gelfProcessor, false);
        String origin = "localhost";
        when(this.headers.get(HttpHeaders.Names.ORIGIN)).thenReturn(origin);

        handler.messageReceived(ctx, evt);

        ArgumentCaptor<HttpResponse> argument = ArgumentCaptor.forClass(HttpResponse.class);
        verify(channel).write(argument.capture());
        verify(this.gelfProcessor).messageReceived(any(GELFMessage.class), eq(this.messageInput));

        HttpResponse response = argument.getValue();
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertNull(response.headers().get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS));
    }
}
