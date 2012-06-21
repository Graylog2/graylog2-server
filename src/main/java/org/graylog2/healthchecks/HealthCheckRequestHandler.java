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

package org.graylog2.healthchecks;

import com.yammer.metrics.HealthChecks;
import com.yammer.metrics.core.HealthCheck.Result;
import java.util.Map;
import java.util.Map.Entry;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

/**
 * HealthCheckRequestHandler.java: 19.06.2012 16:51:09
 *
 * Describe me.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class HealthCheckRequestHandler extends SimpleChannelUpstreamHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        StringBuilder buffer = new StringBuilder();
        HttpRequest request = (HttpRequest) e.getMessage();

        buffer.setLength(0);
        buffer.append(buildTextResponse());
        buffer.append("\r\n");

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setContent(ChannelBuffers.copiedBuffer(buffer.toString(), CharsetUtil.UTF_8));
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");

        // Write.
        ChannelFuture future = e.getChannel().write(response);

        // Close.
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private String buildTextResponse() {
        StringBuilder response = new StringBuilder();

        final Map<String, Result> results = HealthChecks.runHealthChecks();
        for (Entry<String, Result> entry : results.entrySet()) {
            response.append(entry.getKey());

            if (entry.getValue().isHealthy()) {
                response.append(":OK");
            } else {
                response.append(":FAIL");
            }

            response.append(" [").append(entry.getValue().getMessage()).append("]");
            response.append("\n");
        }

        return response.toString();
    }

}
