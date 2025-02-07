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

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.shared.rest.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.graylog.grpc.Constants.AUTHENTICATED_SUBJECT;
import static org.graylog2.shared.security.ShiroRequestHeadersBinder.REQUEST_HEADERS;

@Singleton
public class CallAuthorizer {
    private static final Logger log = LoggerFactory.getLogger(CallAuthorizer.class);

    /**
     * Checks if the current subject has the given permission.
     */
    public boolean isPermitted(String permission) {
        final Subject subject = AUTHENTICATED_SUBJECT.get();

        if (subject == null) {
            log.error("Empty subject in current gRPC context.");
            return false;
        }

        try {
            setRequestIdHeader();
            return subject.isPermitted(permission);
        } finally {
            removeRequestIdHeader();
        }

    }

    /**
     * Verifies that the current subject has the given permissions. If the check fails,
     * {@link StreamObserver#onError(Throwable)} will be called.
     *
     * @param responseObserver The response observer to call {@link StreamObserver#onError(Throwable)} on in case the
     *                         subject doesn't have the required permissions.
     * @param permissions      The permissions to check.
     * @return False if the subject does not have the required permissions (and
     * {@link StreamObserver#onError(Throwable)} was called). True otherwise.
     */
    public boolean verifyPermitted(StreamObserver<?> responseObserver, String... permissions) {
        final boolean permitted = Arrays.stream(permissions).allMatch(this::isPermitted);
        if (!permitted) {
            responseObserver.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
        }
        return permitted;
    }

    /**
     * Our authentication stack expects a X-Request-Id header to be present in the thread context. If the header is not
     * present, the {@link org.graylog2.security.realm.MongoDbAuthorizationRealm} will log a warning when trying to
     * compute a cache key.
     * <p>
     * In a web context, the header is set to an individual id for every request, which will scope caching for auth
     * calls to a single request.
     * <p>
     * In a gRPC context, we don't really have the requirement of limiting the caching scope in that manner. The default
     * behaviour of caching per principal with a short expiration time is just fine.
     * <p>
     * By setting the header to a static value we will get that default behaviour.
     */
    private void setRequestIdHeader() {
        final MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.add(RequestIdFilter.X_REQUEST_ID, "gRPC-request");
        ThreadContext.put(REQUEST_HEADERS, requestHeaders);
    }

    private void removeRequestIdHeader() {
        ThreadContext.remove(REQUEST_HEADERS);
    }
}
