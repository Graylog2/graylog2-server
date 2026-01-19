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
package org.graylog.plugins.sidecar.opamp.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Authenticates OpAMP WebSocket connections during HTTP upgrade.
 * Uses the same token-based authentication as the sidecar REST API.
 *
 * Expected header format:
 *   Authorization: Basic base64(token_value:token)
 *
 * Where:
 *   - token_value is the actual access token string
 *   - "token" is the literal string indicating token-based auth
 */
@Singleton
@ChannelHandler.Sharable
public class OpAMPAuthHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(OpAMPAuthHandler.class);
    private static final String BASIC_PREFIX = "Basic ";

    private final AccessTokenService accessTokenService;
    private final UserService userService;

    @Inject
    public OpAMPAuthHandler(AccessTokenService accessTokenService, UserService userService) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest request) {
            final String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);

            if (!authenticate(authHeader)) {
                LOG.warn("OpAMP authentication failed from {}", ctx.channel().remoteAddress());
                // Release the request to avoid ByteBuf leak
                ReferenceCountUtil.release(msg);
                sendUnauthorized(ctx);
                return;
            }

            LOG.debug("OpAMP authentication successful from {}", ctx.channel().remoteAddress());
        }

        // Pass through to next handler
        super.channelRead(ctx, msg);
    }

    private boolean authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
            LOG.debug("Missing or invalid Authorization header (present: {}, valid prefix: {})",
                    authHeader != null, authHeader != null && authHeader.startsWith(BASIC_PREFIX));
            return false;
        }

        try {
            final String base64Credentials = authHeader.substring(BASIC_PREFIX.length());
            final String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            final String[] parts = credentials.split(":", 2);

            if (parts.length != 2) {
                LOG.debug("Invalid credentials format");
                return false;
            }

            final String tokenValue = parts[0];
            final String credential = parts[1];

            // Check if this is token-based auth (credential == "token")
            if (!"token".equalsIgnoreCase(credential)) {
                LOG.debug("Non-token credential type: {}", credential);
                return false;
            }

            // Load and validate the access token
            final AccessToken accessToken = accessTokenService.load(tokenValue);
            if (accessToken == null) {
                LOG.debug("Access token not found");
                return false;
            }

            // Load and validate the user
            final User user = userService.load(accessToken.getUserName());
            if (user == null) {
                LOG.debug("User not found for token");
                return false;
            }

            if (!user.getAccountStatus().equals(User.AccountStatus.ENABLED)) {
                LOG.warn("Account for user <{}> is disabled", user.getName());
                return false;
            }

            // Update last access time
            try {
                accessTokenService.touch(accessToken);
            } catch (ValidationException e) {
                LOG.warn("Unable to update access token's last access date", e);
            }

            LOG.debug("Authenticated OpAMP connection for user: {}", user.getName());
            return true;

        } catch (IllegalArgumentException e) {
            LOG.debug("Failed to decode Authorization header", e);
            return false;
        }
    }

    private void sendUnauthorized(ChannelHandlerContext ctx) {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.UNAUTHORIZED
        );
        response.headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"OpAMP\"");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        ctx.writeAndFlush(response).addListener(future -> ctx.close());
    }
}
