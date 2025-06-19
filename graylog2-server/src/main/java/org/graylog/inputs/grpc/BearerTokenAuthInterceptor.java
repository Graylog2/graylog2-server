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
package org.graylog.inputs.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Blocks gRPC calls if there is no authorization header with a valid bearer token
 */
public class BearerTokenAuthInterceptor implements ServerInterceptor {
    private static final Logger LOG = getLogger(BearerTokenAuthInterceptor.class);

    private final String staticToken;

    public BearerTokenAuthInterceptor(String staticToken) {
        this.staticToken = staticToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        final var authHeader = StringUtils.strip(
                headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)));

        if (StringUtils.isEmpty(authHeader)) {
            return closedCall(call, headers, "\"Authorization\" header is missing");
        }

        final var token = StringUtils.removeStart(authHeader, "Bearer ");
        if (token.length() == authHeader.length() || token.isEmpty()) {
            return closedCall(call, headers, "\"Authorization\" header doesn't contain a bearer token");
        }

        if (staticToken.equals(token)) {
            // authentication successful, proceed with the call
            return Contexts.interceptCall(Context.current(), call, headers, next);
        }

        return closedCall(call, headers, "Bearer token is invalid");
    }

    private static <ReqT, RespT> ServerCall.Listener<ReqT> closedCall(
            ServerCall<ReqT, RespT> call, Metadata headers, String reason) {

        // Respond with a generic description to not reveal any details, but log the actual reason for debugging.
        LOG.debug("Authentication for gRPC call {} failed: {}.", call.getMethodDescriptor().getFullMethodName(), reason);
        call.close(Status.UNAUTHENTICATED.withDescription("Authentication required"), headers);
        return new ServerCall.Listener<>() {};
    }
}
