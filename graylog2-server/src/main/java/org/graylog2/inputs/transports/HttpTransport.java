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

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;

import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jboss.netty.channel.Channels.fireMessageReceived;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;

public class HttpTransport extends AbstractTcpTransport {
    static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;
    static final int DEFAULT_MAX_HEADER_SIZE = 8192;
    static final int DEFAULT_MAX_CHUNK_SIZE = (int) Size.kilobytes(64L).toBytes();
    static final int DEFAULT_IDLE_WRITER_TIMEOUT = 60;

    static final String CK_ENABLE_CORS = "enable_cors";
    static final String CK_MAX_CHUNK_SIZE = "max_chunk_size";
    static final String CK_IDLE_WRITER_TIMEOUT = "idle_writer_timeout";

    private final boolean enableCors;
    private final HashedWheelTimer timer;
    private final int maxChunkSize;
    private final int idleWriterTimeout;

    @AssistedInject
    public HttpTransport(@Assisted Configuration configuration,
                         @Named("bossPool") Executor bossPool,
                         ThroughputCounter throughputCounter,
                         ConnectionCounter connectionCounter,
                         HashedWheelTimer timer,
                         LocalMetricRegistry localRegistry) {
        super(configuration,
              throughputCounter,
              localRegistry,
              bossPool,
              executorService("worker", "http-transport-worker-%d", localRegistry),
              connectionCounter);

        enableCors = configuration.getBoolean(CK_ENABLE_CORS);

        this.timer = timer;
        int maxChunkSize = configuration.intIsSet(CK_MAX_CHUNK_SIZE) ? configuration.getInt(CK_MAX_CHUNK_SIZE) : DEFAULT_MAX_CHUNK_SIZE;
        this.maxChunkSize = maxChunkSize <= 0 ? DEFAULT_MAX_CHUNK_SIZE : maxChunkSize;
        this.idleWriterTimeout = configuration.intIsSet(CK_IDLE_WRITER_TIMEOUT) ? configuration.getInt(CK_IDLE_WRITER_TIMEOUT, DEFAULT_IDLE_WRITER_TIMEOUT) : DEFAULT_IDLE_WRITER_TIMEOUT;
    }

    private static Executor executorService(final String executorName, final String threadNameFormat, final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNameFormat).build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                metricRegistry,
                name(HttpTransport.class, executorName, "executor-service"));
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> baseChannelHandlers =
                super.getBaseChannelHandlers(input);

        if (idleWriterTimeout > 0) {
            // Install read timeout handler to close idle connections after a timeout.
            // This avoids dangling HTTP connections when the HTTP client does not close the connection properly.
            // For details see: https://github.com/Graylog2/graylog2-server/issues/3223#issuecomment-270350500

            baseChannelHandlers.put("read-timeout-handler", () -> new ReadTimeoutHandler(timer, idleWriterTimeout, TimeUnit.SECONDS));
        }

        baseChannelHandlers.put("decoder", () -> new HttpRequestDecoder(DEFAULT_MAX_INITIAL_LINE_LENGTH, DEFAULT_MAX_HEADER_SIZE, maxChunkSize));
        baseChannelHandlers.put("aggregator", () -> new HttpChunkAggregator(maxChunkSize));
        baseChannelHandlers.put("encoder", HttpResponseEncoder::new);
        baseChannelHandlers.put("decompressor", HttpContentDecompressor::new);

        return baseChannelHandlers;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getFinalChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = Maps.newLinkedHashMap();

        handlers.put("http-handler", () -> new Handler(enableCors));

        handlers.putAll(super.getFinalChannelHandlers(input));
        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<HttpTransport> {
        @Override
        HttpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractTcpTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            r.addField(new BooleanField(CK_ENABLE_CORS,
                                        "Enable CORS",
                                        true,
                                        "Input sends CORS headers to satisfy browser security policies"));
            r.addField(new NumberField(CK_MAX_CHUNK_SIZE,
                                        "Max. HTTP chunk size",
                                        DEFAULT_MAX_CHUNK_SIZE,
                                        "The maximum HTTP chunk size in bytes (e. g. length of HTTP request body)",
                                        ConfigurationField.Optional.OPTIONAL));
            r.addField(new NumberField(CK_IDLE_WRITER_TIMEOUT,
                                        "Idle writer timeout",
                                        DEFAULT_IDLE_WRITER_TIMEOUT,
                                        "The server closes the connection after the given time in seconds after the last client write request. (use 0 to disable)",
                                        ConfigurationField.Optional.OPTIONAL,
                                        NumberField.Attribute.ONLY_POSITIVE));
            return r;
        }
    }
    public static class Handler extends SimpleChannelHandler {

        private final boolean enableCors;

        public Handler(boolean enableCors) {
            this.enableCors = enableCors;
        }

        @Override
        public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
            final Channel channel = e.getChannel();
            final HttpRequest request = (HttpRequest) e.getMessage();
            final boolean keepAlive = isKeepAlive(request);
            final HttpVersion httpRequestVersion = request.getProtocolVersion();
            final String origin = request.headers().get(Names.ORIGIN);

            // to allow for future changes, let's be at least a little strict in what we accept here.
            if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                writeResponse(channel, keepAlive, httpRequestVersion, OK, origin);
                return;
            } else if (!HttpMethod.POST.equals(request.getMethod())) {
                writeResponse(channel, keepAlive, httpRequestVersion, METHOD_NOT_ALLOWED, origin);
                return;
            }

            final ChannelBuffer buffer = request.getContent();

            final boolean correctPath = "/gelf".equals(request.getUri());

            if (!correctPath) {
                writeResponse(channel, keepAlive, httpRequestVersion, NOT_FOUND, origin);
            } else {
                // send on to raw message handler
                writeResponse(channel, keepAlive, httpRequestVersion, ACCEPTED, origin);
                fireMessageReceived(ctx, buffer);
            }
        }

        private void writeResponse(Channel channel,
                                   boolean keepAlive,
                                   HttpVersion httpRequestVersion,
                                   HttpResponseStatus status,
                                   String origin) {
            final HttpResponse response =
                    new DefaultHttpResponse(httpRequestVersion, status);

            response.headers().set(Names.CONTENT_LENGTH, 0);
            response.headers().set(Names.CONNECTION,
                                   keepAlive ? Values.KEEP_ALIVE : Values.CLOSE);

            if (enableCors && origin != null && !origin.isEmpty()) {
                response.headers().set(Names.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                response.headers().set(Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
                response.headers().set(Names.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type");
            }

            final ChannelFuture channelFuture = channel.write(response);
            if (!keepAlive) {
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
