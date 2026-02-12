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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AttributeKey;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;

/**
 * A Netty {@link ChannelInboundHandlerAdapter} that extracts the agent's instance_uid from the
 * client certificate CN after TLS handshake and stores it as a channel attribute.
 * <p>
 * This handler listens for {@link SslHandshakeCompletionEvent} and, on success, retrieves the
 * peer certificate from the {@link SslHandler}'s SSL session. The extracted CN is stored under
 * {@link #AGENT_INSTANCE_UID} for later handlers to read on a per-request basis.
 */
public class AgentCertChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(AgentCertChannelHandler.class);

    public static final AttributeKey<String> AGENT_INSTANCE_UID =
            AttributeKey.valueOf("agent-instance-uid");

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent event && event.isSuccess()) {
            final SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
            final SSLSession session = sslHandler.engine().getSession();
            try {
                final X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                ctx.channel().attr(AGENT_INSTANCE_UID).set(PemUtils.extractCn(cert));
            } catch (Exception e) {
                LOG.warn("Failed to extract agent identity from client certificate", e);
            }
        }
        ctx.fireUserEventTriggered(evt);
    }
}
