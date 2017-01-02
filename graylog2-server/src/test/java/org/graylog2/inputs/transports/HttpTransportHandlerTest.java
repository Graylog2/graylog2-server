/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpTransportHandlerTest {
    private DecoderEmbedder<HttpResponse> channel;

    @Before
    public void setUp() throws Exception {
        final SimpleChannelHandler channelHandler = new HttpTransport.Handler(true);
        channel = new DecoderEmbedder<>(channelHandler);
    }

    @Test
    public void messageReceivedSuccessfullyProcessesPOSTRequest() throws Exception {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/gelf");
        httpRequest.headers().add("Host", "localhost");
        httpRequest.headers().add("Origin", "http://example.com");
        httpRequest.headers().add("Connection", "close");

        final String gelfMessage = "{\"version\":\"1.1\",\"short_message\":\"Foo\",\"host\":\"localhost\"}";
        httpRequest.setContent(ChannelBuffers.copiedBuffer(gelfMessage.toCharArray(), StandardCharsets.UTF_8));

        channel.offer(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.poll();
        assertThat(httpResponse.getStatus()).isEqualTo(HttpResponseStatus.ACCEPTED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void messageReceivedSuccessfullyProcessesOPTIONSRequest() throws Exception {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/gelf");
        httpRequest.headers().add("Host", "localhost");
        httpRequest.headers().add("Origin", "http://example.com");
        httpRequest.headers().add("Connection", "close");

        channel.offer(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.poll();
        assertThat(httpResponse.getStatus()).isEqualTo(HttpResponseStatus.OK);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void messageReceivedReturns404ForWrongPath() throws Exception {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
        httpRequest.headers().add("Host", "localhost");
        httpRequest.headers().add("Origin", "http://example.com");
        httpRequest.headers().add("Connection", "close");

        channel.offer(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.poll();
        assertThat(httpResponse.getStatus()).isEqualTo(HttpResponseStatus.NOT_FOUND);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }

    @Test
    public void messageReceivedReturns405ForInvalidMethod() throws Exception {
        final HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        httpRequest.headers().add("Host", "localhost");
        httpRequest.headers().add("Origin", "http://example.com");
        httpRequest.headers().add("Connection", "close");

        channel.offer(httpRequest);
        channel.finish();

        final HttpResponse httpResponse = channel.poll();
        assertThat(httpResponse.getStatus()).isEqualTo(HttpResponseStatus.METHOD_NOT_ALLOWED);
        final HttpHeaders headers = httpResponse.headers();
        assertThat(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).isEqualTo("0");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.get(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("Authorization, Content-Type");
    }
}