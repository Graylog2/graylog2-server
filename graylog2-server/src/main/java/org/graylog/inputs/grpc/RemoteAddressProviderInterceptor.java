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
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import java.net.SocketAddress;

/**
 * Adds a REMOTE_ADDRESS context key containing the remote address, if present as a transport attribute
 */
public class RemoteAddressProviderInterceptor implements ServerInterceptor {
    public static final Context.Key<SocketAddress> REMOTE_ADDRESS = Context.key("Remote address");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        final var remoteAddr = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (remoteAddr != null) {
            return Contexts.interceptCall(Context.current().withValue(REMOTE_ADDRESS, remoteAddr), call, headers, next);
        }
        return Contexts.interceptCall(Context.current(), call, headers, next);
    }
}
