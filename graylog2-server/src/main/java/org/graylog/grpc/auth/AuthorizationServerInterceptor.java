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
package org.graylog.grpc.auth;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Set;

import static org.graylog.grpc.Constants.AUTHENTICATED_SUBJECT;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Intercepts a call and checks the API token provided in the call credentials. Closes a call if the token can not be
 * authenticated. If authentication is successful, the call is allowed to proceed with the shiro {@link Subject} set
 * in the current gRPC {@link Context}.
 */
public class AuthorizationServerInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerInterceptor.class);

    private final ShiroAuthenticator shiroAuthenticator;
    private final Set<MethodDescriptor<?, ?>> bypassMethods;
    private final TokenExtractor tokenExtractor;

    public interface Factory {
        AuthorizationServerInterceptor create(Set<MethodDescriptor<?, ?>> bypassMethods, TokenExtractor tokenExtractor);
    }

    @AssistedInject
    public AuthorizationServerInterceptor(ShiroAuthenticator shiroAuthenticator,
                                          @Assisted Set<MethodDescriptor<?, ?>> bypassMethods,
                                          @Assisted TokenExtractor tokenExtractor) {
        this.shiroAuthenticator = shiroAuthenticator;
        this.bypassMethods = bypassMethods;
        this.tokenExtractor = tokenExtractor;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {

        if (bypassMethods.contains(call.getMethodDescriptor())) {
            return Contexts.interceptCall(Context.current(), call, headers, next);
        }

        final String token;
        try {
            final var t = tokenExtractor.extract(headers);
            if (t.isPresent()) {
                token = t.get();
            } else {
                call.close(Status.UNAUTHENTICATED.withDescription("Missing API token."), headers);
                return new ServerCall.Listener<>() {};
            }
        } catch (IllegalArgumentException e) {
            call.close(Status.UNAUTHENTICATED.withDescription(
                    f("Missing API token: %s", e.getLocalizedMessage())), headers);
            return new ServerCall.Listener<>() {};
        }

        if (StringUtils.isBlank(token)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing API token."), headers);
            return new ServerCall.Listener<>() {};
        }

        // not really a host but we don't really care much about the format here
        final String host = getRemoteAddr(call);

        try {
            final Subject subject = shiroAuthenticator.authenticate(host, token);
            final Context ctx = Context.current().withValue(AUTHENTICATED_SUBJECT, subject);
            return Contexts.interceptCall(ctx, call, headers, next);
        } catch (AuthenticationException e) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid API token."), headers);
        } catch (Exception e) {
            log.error("Authentication failed due to an internal error.", e);
            call.close(Status.INTERNAL.withDescription("Internal error during call authentication."), headers);
        }
        return new ServerCall.Listener<>() {};
    }

    private <ReqT, RespT> String getRemoteAddr(ServerCall<ReqT, RespT> call) {
        final SocketAddress socketAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        return socketAddress == null ? "" : socketAddress.toString();
    }
}
