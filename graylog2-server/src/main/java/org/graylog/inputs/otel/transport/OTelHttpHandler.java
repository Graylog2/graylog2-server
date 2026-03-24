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
package org.graylog.inputs.otel.transport;

import com.google.protobuf.AbstractMessageLite;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog2.inputs.transports.netty.HttpHandler;
import org.graylog2.inputs.transports.netty.RawMessageHandler;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * OTLP HTTP handler for the generic user-facing OTel HTTP input.
 * <p>
 * Extends {@link HttpHandler} to inherit auth, CORS, OPTIONS, method/path validation,
 * and keep-alive handling. Overrides {@link #handleValidPost} to parse OTLP requests
 * and send structured OTLP responses.
 * <p>
 * This handler calls {@link MessageInput#processRawMessage} directly instead of firing
 * messages downstream to {@link RawMessageHandler}. This is intentional — OTLP requires
 * knowing whether processing succeeded before sending the response, but the Netty pipeline
 * doesn't support firing N messages downstream, waiting for all to complete, and then
 * deciding the response. RawMessageHandler (added unconditionally by AbstractTcpTransport)
 * remains in the pipeline but is unreachable.
 */
public class OTelHttpHandler extends HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OTelHttpHandler.class);

    public static final String LOGS_PATH = OtlpHttpUtils.LOGS_PATH;

    private final MessageInput input;

    public OTelHttpHandler(boolean enableCors, String authorizationHeader,
                           String authorizationHeaderValue, String path,
                           MessageInput input) {
        super(enableCors, authorizationHeader, authorizationHeaderValue, path);
        this.input = input;
    }

    @Override
    protected void handleValidPost(ChannelHandlerContext ctx, FullHttpRequest request, boolean keepAlive,
                                    String origin) {
        final boolean isProtobuf = OtlpHttpUtils.isProtobuf(request);

        // 1. Parse request
        final ExportLogsServiceRequest exportRequest;
        try {
            exportRequest = OtlpHttpUtils.parse(request);
        } catch (OtlpHttpUtils.UnsupportedContentTypeException e) {
            writeResponse(ctx.channel(), keepAlive, request.protocolVersion(),
                    HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE, origin);
            return;
        } catch (Exception e) {
            LOG.debug("Failed to parse OTLP request", e);
            writeResponse(ctx.channel(), keepAlive, request.protocolVersion(),
                    HttpResponseStatus.BAD_REQUEST, origin);
            return;
        }

        // 2. Process
        try {
            createJournalRecords(ctx, exportRequest).forEach(input::processRawMessage);
        } catch (Exception e) {
            LOG.error("Failed to process OTLP request", e);
            writeResponse(ctx.channel(), keepAlive, request.protocolVersion(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR, origin);
            return;
        }

        // 3. Respond
        final byte[] responseBody = isProtobuf ? OtlpHttpUtils.SUCCESS_RESPONSE_PROTOBUF : OtlpHttpUtils.SUCCESS_RESPONSE_JSON;
        final String responseContentType = isProtobuf ? OtlpHttpUtils.PROTOBUF_CONTENT_TYPE : OtlpHttpUtils.JSON_CONTENT_TYPE;
        writeResponse(ctx.channel(), keepAlive, request.protocolVersion(),
                HttpResponseStatus.OK, origin, responseBody, responseContentType);
    }

    /**
     * Creates journal records from the parsed OTLP request.
     * Resolves the source IP from the forwarded-for channel attribute if available.
     */
    protected Stream<RawMessage> createJournalRecords(ChannelHandlerContext ctx,
                                                       ExportLogsServiceRequest exportRequest) {
        final InetSocketAddress remoteAddress = resolveRemoteAddress(ctx);
        final Function<byte[], RawMessage> createRawMessage = remoteAddress != null
                ? bytes -> new RawMessage(bytes, remoteAddress)
                : RawMessage::new;

        return OTelJournalRecordFactory.createFromRequest(exportRequest).stream()
                .map(AbstractMessageLite::toByteArray)
                .map(createRawMessage);
    }

    /**
     * Resolves the client IP address, preferring the forwarded-for value set by
     * {@link org.graylog2.inputs.transports.netty.HttpForwardedForHandler}.
     */
    private InetSocketAddress resolveRemoteAddress(ChannelHandlerContext ctx) {
        if (ctx.channel().hasAttr(RawMessageHandler.ORIGINAL_IP_KEY)) {
            return ctx.channel().attr(RawMessageHandler.ORIGINAL_IP_KEY).get();
        }
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress address) {
            return address;
        }
        return null;
    }
}
