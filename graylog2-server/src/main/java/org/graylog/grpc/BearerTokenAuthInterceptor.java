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
package org.graylog.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.apache.commons.lang3.StringUtils;

/**
 * Blocks gRPC calls if there is no authorization header with a valid bearer token
 */
public class BearerTokenAuthInterceptor implements ServerInterceptor {

    private final String staticToken;

    public BearerTokenAuthInterceptor(String staticToken) {
        this.staticToken = staticToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        final var authHeader = StringUtils.strip(
                headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)));

        if (StringUtils.isEmpty(authHeader)) {
            return closedCallWithDescription(call, headers, "Missing bearer token.");
        }

        final var token = StringUtils.removeStart(authHeader, "Bearer ");
        if (token.length() == authHeader.length() || token.isEmpty()) {
            return closedCallWithDescription(call, headers, "Authorization header doesn't contain a bearer token.");
        }

        if (staticToken.equals(token)) {
            // authentication successful, proceed with the call
            return Contexts.interceptCall(Context.current(), call, headers, next);
        }

        return closedCallWithDescription(call, headers, "Invalid bearer token.");
    }

    private static <ReqT, RespT> ServerCall.Listener<ReqT> closedCallWithDescription(
            ServerCall<ReqT, RespT> call, Metadata headers, String description) {

        call.close(Status.UNAUTHENTICATED.withDescription(description), headers);
        return new ServerCall.Listener<>() {};
    }
}
