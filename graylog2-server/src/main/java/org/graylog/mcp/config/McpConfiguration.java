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
package org.graylog.mcp.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@JsonAutoDetect
@AutoValue
public abstract class McpConfiguration {
    public static final boolean DEFAULT_ENABLE_OAUTH = true;
    public static final Duration DEFAULT_OAUTH_ACCESS_TOKEN_TTL = Duration.ofHours(1);
    public static final Duration DEFAULT_OAUTH_REFRESH_TOKEN_TTL = Duration.ofDays(30);
    public static final List<String> DEFAULT_OAUTH_ALLOWED_REDIRECT_URIS = List.of(
            "https://claude.ai/api/mcp/auth_callback",
            "https://claude.com/api/mcp/auth_callback",
            "https://vscode.dev/redirect"
    );

    public static final McpConfiguration DEFAULT_VALUES = create(false, false, false);

    @JsonProperty("enable_remote_access")
    public abstract boolean enableRemoteAccess();

    @JsonProperty("enable_output_schema")
    public abstract boolean enableOutputSchema();

    @JsonProperty("enable_input_validation")
    public abstract boolean enableInputValidation();

    /**
     * Allows MCP clients to obtain access tokens via the OAuth 2.1 authorization code flow.
     */
    @JsonProperty("enable_oauth")
    public abstract boolean enableOAuth();

    @JsonProperty("oauth_access_token_ttl")
    public abstract Duration oauthAccessTokenTtl();

    @JsonProperty("oauth_refresh_token_ttl")
    public abstract Duration oauthRefreshTokenTtl();

    /**
     * Non-loopback redirect URIs that OAuth clients may register. Loopback URIs are always allowed.
     */
    @JsonProperty("oauth_allowed_redirect_uris")
    public abstract List<String> oauthAllowedRedirectUris();

    public boolean oauthAvailable() {
        return enableRemoteAccess() && enableOAuth();
    }

    public static McpConfiguration create(boolean enableRemoteAccess, boolean enableOutputSchema, boolean enableInputValidation) {
        return create(enableRemoteAccess, enableOutputSchema, enableInputValidation, null, null, null, null);
    }

    @JsonCreator
    public static McpConfiguration create(
            @JsonProperty("enable_remote_access") boolean enableRemoteAccess,
            @JsonProperty("enable_output_schema") boolean enableOutputSchema,
            @JsonProperty("enable_input_validation") boolean enableInputValidation,
            @JsonProperty("enable_oauth") @Nullable Boolean enableOAuth,
            @JsonProperty("oauth_access_token_ttl") @Nullable Duration oauthAccessTokenTtl,
            @JsonProperty("oauth_refresh_token_ttl") @Nullable Duration oauthRefreshTokenTtl,
            @JsonProperty("oauth_allowed_redirect_uris") @Nullable List<String> oauthAllowedRedirectUris
    ) {
        return new AutoValue_McpConfiguration(
                enableRemoteAccess,
                enableOutputSchema,
                enableInputValidation,
                Objects.requireNonNullElse(enableOAuth, DEFAULT_ENABLE_OAUTH),
                Objects.requireNonNullElse(oauthAccessTokenTtl, DEFAULT_OAUTH_ACCESS_TOKEN_TTL),
                Objects.requireNonNullElse(oauthRefreshTokenTtl, DEFAULT_OAUTH_REFRESH_TOKEN_TTL),
                List.copyOf(Objects.requireNonNullElse(oauthAllowedRedirectUris, DEFAULT_OAUTH_ALLOWED_REDIRECT_URIS))
        );
    }
}
