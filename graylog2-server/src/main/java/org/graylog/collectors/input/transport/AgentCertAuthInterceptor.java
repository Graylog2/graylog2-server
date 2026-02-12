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

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * A gRPC {@link ServerInterceptor} that reads the agent's instance_uid from transport
 * {@link io.grpc.Attributes} (set by {@link AgentCertTransportFilter}) and copies it
 * to the gRPC {@link Context} for use by service handlers.
 * <p>
 * If no agent UID is found in the transport attributes, the call is rejected with
 * {@link Status#UNAUTHENTICATED}.
 */
public class AgentCertAuthInterceptor implements ServerInterceptor {
    public static final Context.Key<String> AGENT_INSTANCE_UID =
            Context.key("opamp-agent-instance-uid");

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(
            ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
        final String uid = call.getAttributes().get(AgentCertTransportFilter.AGENT_INSTANCE_UID_KEY);
        if (uid == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("No agent certificate"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        return Contexts.interceptCall(
                Context.current().withValue(AGENT_INSTANCE_UID, uid), call, headers, next);
    }
}
