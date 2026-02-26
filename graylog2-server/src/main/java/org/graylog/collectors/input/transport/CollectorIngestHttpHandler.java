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
package org.graylog.collectors.input.transport;

import com.google.protobuf.AbstractMessageLite;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import org.graylog.collectors.CollectorJournal;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog.inputs.otel.transport.OTelHttpHandler;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An HTTP handler for collector-managed agents that extracts the agent instance UID
 * from the Netty channel attribute (set by {@link AgentCertChannelHandler} during TLS handshake)
 * and embeds it in each journal record before writing to the Graylog journal.
 * <p>
 * If the agent instance UID is not present on the channel (i.e., no valid client certificate),
 * the handler rejects the request with a 401 Unauthorized response.
 */
public class CollectorIngestHttpHandler extends OTelHttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorIngestHttpHandler.class);

    public CollectorIngestHttpHandler(OTelJournalRecordFactory journalRecordFactory, MessageInput input) {
        super(journalRecordFactory, input);
    }

    @Override
    protected boolean validateRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        final String instanceUid = ctx.channel().attr(AgentCertChannelHandler.AGENT_INSTANCE_UID).get();
        if (instanceUid == null) {
            LOG.warn("Rejecting request without agent identity (no valid client certificate)");
            sendError(ctx, HttpResponseStatus.UNAUTHORIZED, HttpUtil.isKeepAlive(request));
            return false;
        }
        return true;
    }

    @Override
    protected Stream<RawMessage> createJournalRecords(ChannelHandlerContext ctx,
                                                      ExportLogsServiceRequest exportRequest) {
        final String instanceUid = ctx.channel().attr(AgentCertChannelHandler.AGENT_INSTANCE_UID).get();

        final Function<byte[], RawMessage> createRawMessage;
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress address) {
            createRawMessage = bytes -> new RawMessage(bytes, address);
        } else {
            createRawMessage = RawMessage::new;
        }

        return journalRecordFactory.createFromRequest(exportRequest).stream()
                .map(otelRecord -> CollectorJournal.Record.newBuilder()
                        .setOtelRecord(otelRecord)
                        .setCollectorInstanceUid(instanceUid)
                        .build())
                .map(AbstractMessageLite::toByteArray)
                .map(createRawMessage);
    }
}
