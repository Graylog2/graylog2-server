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
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jboss.netty.channel.Channels.fireMessageReceived;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

public class HttpTransport extends AbstractTcpTransport {
    static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;
    static final int DEFAULT_MAX_HEADER_SIZE = 8192;
    static final int DEFAULT_MAX_CHUNK_SIZE = (int) Size.kilobytes(64L).toBytes();

    static final String CK_ENABLE_CORS = "enable_cors";
    static final String CK_MAX_CHUNK_SIZE = "max_chunk_size";

    private final boolean enableCors;
    private final int maxChunkSize;

    @AssistedInject
    public HttpTransport(@Assisted Configuration configuration,
                         @Named("bossPool") Executor bossPool,
                         ThroughputCounter throughputCounter,
                         ConnectionCounter connectionCounter,
                         LocalMetricRegistry localRegistry) {
        super(configuration,
              throughputCounter,
              localRegistry,
              bossPool,
              executorService("worker", "http-transport-worker-%d", localRegistry),
              connectionCounter);

        enableCors = configuration.getBoolean(CK_ENABLE_CORS);

        int maxChunkSize = configuration.intIsSet(CK_MAX_CHUNK_SIZE) ? configuration.getInt(CK_MAX_CHUNK_SIZE) : DEFAULT_MAX_CHUNK_SIZE;
        this.maxChunkSize = maxChunkSize <= 0 ? DEFAULT_MAX_CHUNK_SIZE : maxChunkSize;
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

        baseChannelHandlers.put("decoder", new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                return new HttpRequestDecoder(DEFAULT_MAX_INITIAL_LINE_LENGTH, DEFAULT_MAX_HEADER_SIZE, maxChunkSize);
            }
        });
        baseChannelHandlers.put("encoder", new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                return new HttpResponseEncoder();
            }
        });
        baseChannelHandlers.put("decompressor", new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                return new HttpContentDecompressor();
            }
        });

        return baseChannelHandlers;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getFinalChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = Maps.newLinkedHashMap();

        handlers.put("http-handler", new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                return new Handler(enableCors);
            }
        });

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
            if (request.getMethod() != HttpMethod.POST) {
                writeResponse(channel, keepAlive, httpRequestVersion, METHOD_NOT_ALLOWED, origin);
                return;
            }

            final ChannelBuffer buffer = request.getContent();

            if ("/gelf".equals(request.getUri())) {
                // send on to raw message handler
                writeResponse(channel, keepAlive, httpRequestVersion, ACCEPTED, origin);
                fireMessageReceived(ctx, buffer);
            } else {
                writeResponse(channel, keepAlive, httpRequestVersion, NOT_FOUND, origin);
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

            if (enableCors) {
                if (origin != null && !origin.isEmpty()) {
                    response.headers().set(Names.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                    response.headers().set(Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
                    response.headers().set(Names.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization");
                }
            }

            final ChannelFuture channelFuture = channel.write(response);
            if (!keepAlive) {
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

}
