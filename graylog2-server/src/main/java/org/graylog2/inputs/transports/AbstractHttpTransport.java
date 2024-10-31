/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs.transports;

import com.github.joschi.jadconfig.util.Size;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jakarta.annotation.Nullable;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.inputs.transports.netty.HttpHandler;
import org.graylog2.inputs.transports.netty.LenientDelimiterBasedFrameDecoder;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.transports.AbstractTcpTransport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.graylog2.shared.utilities.StringUtils.f;

abstract public class AbstractHttpTransport extends AbstractTcpTransport {
    private static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;
    private static final int DEFAULT_MAX_HEADER_SIZE = 8192;
    protected static final int DEFAULT_MAX_CHUNK_SIZE = (int) Size.kilobytes(64L).toBytes();
    private static final int DEFAULT_IDLE_WRITER_TIMEOUT = 60;

    static final String CK_ENABLE_BULK_RECEIVING = "enable_bulk_receiving";
    static final String CK_ENABLE_CORS = "enable_cors";
    static final String CK_MAX_CHUNK_SIZE = "max_chunk_size";
    static final String CK_IDLE_WRITER_TIMEOUT = "idle_writer_timeout";
    static final String CK_AUTHORIZATION_HEADER_NAME = "authorization_header_name";
    static final String CK_AUTHORIZATION_HEADER_VALUE = "authorization_header_value";
    private static final String AUTHORIZATION_HEADER_NAME_LABEL = "Authorization Header Name";
    private static final String AUTHORIZATION_HEADER_VALUE_LABEL = "Authorization Header Value";

    protected final boolean enableBulkReceiving;
    protected final boolean enableCors;
    protected final int maxChunkSize;
    private final int idleWriterTimeout;
    private final String authorizationHeader;
    private final String authorizationHeaderValue;
    private final String path;

    public AbstractHttpTransport(Configuration configuration,
                                 EventLoopGroup eventLoopGroup,
                                 EventLoopGroupFactory eventLoopGroupFactory,
                                 NettyTransportConfiguration nettyTransportConfiguration,
                                 ThroughputCounter throughputCounter,
                                 LocalMetricRegistry localRegistry,
                                 TLSProtocolsConfiguration tlsConfiguration, String path) {
        super(configuration,
                throughputCounter,
                localRegistry,
                eventLoopGroup,
                eventLoopGroupFactory,
                nettyTransportConfiguration,
                tlsConfiguration);
        this.enableBulkReceiving = configuration.getBoolean(CK_ENABLE_BULK_RECEIVING);
        this.enableCors = configuration.getBoolean(CK_ENABLE_CORS);
        this.maxChunkSize = parseMaxChunkSize(configuration);
        this.idleWriterTimeout = configuration.intIsSet(CK_IDLE_WRITER_TIMEOUT) ? configuration.getInt(CK_IDLE_WRITER_TIMEOUT, DEFAULT_IDLE_WRITER_TIMEOUT) : DEFAULT_IDLE_WRITER_TIMEOUT;
        this.authorizationHeader = configuration.getString(CK_AUTHORIZATION_HEADER_NAME);
        this.authorizationHeaderValue = configuration.getString(CK_AUTHORIZATION_HEADER_VALUE);
        this.path = path;
    }

    /**
     * @return If the configured Max Chunk Size is less than zero, return {@link AbstractHttpTransport#DEFAULT_MAX_CHUNK_SIZE}.
     */
    protected static int parseMaxChunkSize(Configuration configuration) {
        int maxChunkSize = configuration.getInt(CK_MAX_CHUNK_SIZE, DEFAULT_MAX_CHUNK_SIZE);
        return maxChunkSize <= 0 ? DEFAULT_MAX_CHUNK_SIZE : maxChunkSize;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>();
        if (idleWriterTimeout > 0) {
            // Install read timeout handler to close idle connections after a timeout.
            // This avoids dangling HTTP connections when the HTTP client does not close the connection properly.
            // For details see: https://github.com/Graylog2/graylog2-server/issues/3223#issuecomment-270350500
            handlers.put("read-timeout-handler", () -> new ReadTimeoutHandler(idleWriterTimeout, TimeUnit.SECONDS));
        }

        handlers.put("decoder", () -> new HttpRequestDecoder(DEFAULT_MAX_INITIAL_LINE_LENGTH, DEFAULT_MAX_HEADER_SIZE, maxChunkSize));
        handlers.put("decompressor", HttpContentDecompressor::new);
        handlers.put("encoder", HttpResponseEncoder::new);
        handlers.put("aggregator", () -> new HttpObjectAggregator(maxChunkSize));
        handlers.put("http-handler", () -> new HttpHandler(enableCors, authorizationHeader, authorizationHeaderValue, path));
        if (enableBulkReceiving) {
            handlers.put("http-bulk-newline-decoder",
                    () -> new LenientDelimiterBasedFrameDecoder(maxChunkSize, Delimiters.lineDelimiter()));
        }
        handlers.putAll(super.getCustomChildChannelHandlers(input));
        return handlers;
    }

    @Override
    public void launch(MessageInput input, @Nullable InputFailureRecorder inputFailureRecorder) throws MisfireException {
        if (isNotBlank(authorizationHeader) && isBlank(authorizationHeaderValue)) {
            checkForConfigFieldDependencies(AUTHORIZATION_HEADER_NAME_LABEL, AUTHORIZATION_HEADER_VALUE_LABEL);
        } else if (isNotBlank(authorizationHeaderValue) && isBlank(authorizationHeader)) {
            checkForConfigFieldDependencies(AUTHORIZATION_HEADER_VALUE_LABEL, AUTHORIZATION_HEADER_NAME_LABEL);
        }
        super.launch(input, inputFailureRecorder);
    }

    private void checkForConfigFieldDependencies(String configParam1, String configParam2) throws MisfireException {
        throw new MisfireException(f("The [%s] configuration parameter cannot be used without also specifying a value for [%s].", configParam1, configParam2));
    }

    @ConfigClass
    public static class Config extends AbstractTcpTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            r.addField(new BooleanField(CK_ENABLE_BULK_RECEIVING,
                    "Enable Bulk Receiving",
                    false,
                    "Enables bulk receiving of messages separated by newlines (\\n or \\r\\n)"));
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
            r.addField(new TextField(
                    CK_AUTHORIZATION_HEADER_NAME,
                    AUTHORIZATION_HEADER_NAME_LABEL,
                    "",
                    "The name for the authorization header to use. If specified, all requests must contain this header with the correct value to authenticate successfully.",
                    ConfigurationField.Optional.OPTIONAL));
            r.addField(new TextField(
                    CK_AUTHORIZATION_HEADER_VALUE,
                    AUTHORIZATION_HEADER_VALUE_LABEL,
                    "",
                    "The secret authorization header value which all request must have in order to authenticate successfully. e.g. Bearer: <api-token>N",
                    ConfigurationField.Optional.OPTIONAL,
                    TextField.Attribute.IS_PASSWORD));
            return r;
        }
    }
}
