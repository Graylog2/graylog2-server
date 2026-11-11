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

import java.util.Optional;

/**
 * An OAuth error as defined in RFC 6749 section 5.2. The description is meant to be returned to clients.
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
    private final String description;
    private final Response.Status status;
    @Nullable
    private final String redirectUri;

    private OAuthException(String error, String description, Response.Status status, @Nullable String redirectUri) {
        super(description);
        this.error = error;
        this.description = description;
        this.status = status;
        this.redirectUri = redirectUri;
    }

    public static OAuthException of(String error, String description) {
        return new OAuthException(error, description, Response.Status.BAD_REQUEST, null);
    }

    public static OAuthException of(String error, String description, Response.Status status) {
        return new OAuthException(error, description, status, null);
    }

    public static OAuthException invalidClient(String description) {
        return new OAuthException(INVALID_CLIENT, description, Response.Status.UNAUTHORIZED, null);
    }

    /**
     * Marks this authorization request error as safe to report to the client's validated redirect URI.
     */
    public OAuthException redirectTo(String redirectUri) {
        return new OAuthException(error, description, status, redirectUri);
    }

    public String error() {
        return error;
    }

    public String description() {
        return description;
    }

    public Response.Status status() {
        return status;
    }

    public Optional<String> redirectUri() {
        return Optional.ofNullable(redirectUri);
    }
}
