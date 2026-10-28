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
package org.graylog.mcp.oauth;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;

/**
 * An OAuth error as defined in RFC 6749 section 5.2.
 */
public class OAuthException extends RuntimeException {
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    public static final String INVALID_TARGET = "invalid_target";
    public static final String INVALID_REDIRECT_URI = "invalid_redirect_uri";
    public static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

    private final String error;
    private final Response.Status status;
    private final boolean redirectable;

    private OAuthException(String error, String description, Response.Status status, boolean redirectable) {
        super(description);
        this.error = error;
        this.status = status;
        this.redirectable = redirectable;
    }

    public static OAuthException of(String error, String description) {
        return new OAuthException(error, description, Response.Status.BAD_REQUEST, false);
    }

    public static OAuthException of(String error, String description, Response.Status status) {
        return new OAuthException(error, description, status, false);
    }

    /**
     * An authorization request error that can safely be reported to the client's redirect URI.
     */
    public static OAuthException redirectable(String error, String description) {
        return new OAuthException(error, description, Response.Status.BAD_REQUEST, true);
    }

    public static OAuthException invalidClient(String description) {
        return new OAuthException(INVALID_CLIENT, description, Response.Status.UNAUTHORIZED, false);
    }

    public String error() {
        return error;
    }

    @Nullable
    public String description() {
        return getMessage();
    }

    public Response.Status status() {
        return status;
    }

    public boolean redirectable() {
        return redirectable;
    }
}
