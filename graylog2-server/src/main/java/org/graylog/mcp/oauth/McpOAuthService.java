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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.server.McpRestResource;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.PeriodDuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog.mcp.oauth.OAuthException.ACCESS_DENIED;
import static org.graylog.mcp.oauth.OAuthException.INVALID_CLIENT_METADATA;
import static org.graylog.mcp.oauth.OAuthException.INVALID_GRANT;
import static org.graylog.mcp.oauth.OAuthException.INVALID_REDIRECT_URI;
import static org.graylog.mcp.oauth.OAuthException.INVALID_REQUEST;
import static org.graylog.mcp.oauth.OAuthException.INVALID_TARGET;
import static org.graylog.mcp.oauth.OAuthException.TEMPORARILY_UNAVAILABLE;
import static org.graylog.mcp.oauth.OAuthException.UNSUPPORTED_RESPONSE_TYPE;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_AUTHORIZATION_DENY;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_AUTHORIZATION_GRANT;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_AUTHORIZATION_REVOKE;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_CLIENT_REGISTER;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_REFRESH_TOKEN_REUSE;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_TOKEN_ISSUE;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_TOKEN_REFRESH;
import static org.graylog2.audit.AuditEventTypes.MCP_OAUTH_TOKEN_REVOKE;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * OAuth 2.1 authorization server for MCP clients, following the MCP authorization specification.
 * <p>
 * Only public clients using the authorization code grant with PKCE ({@code S256}) are supported. Issued access tokens
 * are regular Graylog access tokens restricted to the MCP endpoint.
 */
@Singleton
public class McpOAuthService {
    private static final Logger LOG = LoggerFactory.getLogger(McpOAuthService.class);

    public static final String RESPONSE_TYPE_CODE = "code";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";
    public static final String TOKEN_ENDPOINT_AUTH_METHOD_NONE = "none";

    static final Duration AUTHORIZATION_CODE_TTL = Duration.ofMinutes(1);
    static final Duration UNUSED_CLIENT_TTL = Duration.ofDays(1);
    static final long MAX_CLIENTS = 10_000;
    private static final int MAX_REDIRECT_URIS = 10;
    private static final int MAX_CLIENT_NAME_LENGTH = 100;
    private static final String DEFAULT_CLIENT_NAME = "Unnamed MCP client";

    private final OAuthClientService clientService;
    private final OAuthAuthorizationCodeService codeService;
    private final OAuthRefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final UserService userService;
    private final AuditEventSender auditEventSender;
    private final Clock clock;

    @Inject
    public McpOAuthService(OAuthClientService clientService,
                           OAuthAuthorizationCodeService codeService,
                           OAuthRefreshTokenService refreshTokenService,
                           AccessTokenService accessTokenService,
                           UserService userService,
                           AuditEventSender auditEventSender,
                           Clock clock) {
        this.clientService = clientService;
        this.codeService = codeService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.auditEventSender = auditEventSender;
        this.clock = clock;
    }

    public record ClientRegistrationRequest(List<String> redirectUris,
                                            @Nullable String clientName,
                                            @Nullable String clientUri,
                                            @Nullable List<String> grantTypes,
                                            @Nullable List<String> responseTypes) {}

    public record AuthorizationRequest(OAuthClient client,
                                       String redirectUri,
                                       String codeChallenge,
                                       @Nullable String state,
                                       @Nullable String resource) {}

    public record TokenResponse(String accessToken, long expiresIn, String refreshToken) {}

    public record Grant(String clientId, String clientName, Instant lastRefreshedAt, Instant expiresAt) {}

    /**
     * Dynamic client registration (RFC 7591). All clients are registered as public clients.
     */
    public OAuthClient registerClient(ClientRegistrationRequest request, McpConfiguration config) {
        final List<String> redirectUris = Optional.ofNullable(request.redirectUris()).orElse(List.of());
        if (redirectUris.isEmpty() || redirectUris.size() > MAX_REDIRECT_URIS) {
            throw OAuthException.of(INVALID_REDIRECT_URI, f("Between 1 and %d redirect URIs are required", MAX_REDIRECT_URIS));
        }
        for (final String redirectUri : redirectUris) {
            if (!RedirectUriPolicy.isAllowed(redirectUri, config.oauthAllowedRedirectUris())) {
                throw OAuthException.of(INVALID_REDIRECT_URI, f("Redirect URI <%s> is not allowed", redirectUri));
            }
        }
        if (request.grantTypes() != null && !Set.of(GRANT_TYPE_AUTHORIZATION_CODE, GRANT_TYPE_REFRESH_TOKEN).containsAll(request.grantTypes())) {
            throw OAuthException.of(INVALID_CLIENT_METADATA, "Only the authorization_code and refresh_token grant types are supported");
        }
        if (request.responseTypes() != null && !Set.of(RESPONSE_TYPE_CODE).containsAll(request.responseTypes())) {
            throw OAuthException.of(INVALID_CLIENT_METADATA, "Only the code response type is supported");
        }
        if (clientService.count() >= MAX_CLIENTS) {
            LOG.warn("Refusing OAuth client registration, the maximum of {} registered clients is reached.", MAX_CLIENTS);
            throw OAuthException.of(TEMPORARILY_UNAVAILABLE, "Too many registered clients", Response.Status.SERVICE_UNAVAILABLE);
        }

        final Instant now = Instant.now(clock);
        final OAuthClient client = new OAuthClient(
                null,
                f("mcp-%s", UUID.randomUUID()),
                sanitizeClientName(request.clientName()),
                request.clientUri(),
                List.copyOf(redirectUris),
                now,
                now.plus(UNUSED_CLIENT_TTL)
        );
        clientService.create(client);
        LOG.debug("Registered OAuth client <{}> ({}) with redirect URIs {}", client.clientId(), client.clientName(), redirectUris);
        auditEventSender.success(AuditActor.user("anonymous"), MCP_OAUTH_CLIENT_REGISTER, clientContext(client));
        return client;
    }

    /**
     * Validates an authorization request. Errors about the client or redirect URI must not be redirected to the client.
     */
    public AuthorizationRequest validateAuthorizationRequest(@Nullable String clientId,
                                                             @Nullable String redirectUri,
                                                             @Nullable String responseType,
                                                             @Nullable String codeChallenge,
                                                             @Nullable String codeChallengeMethod,
                                                             @Nullable String state,
                                                             @Nullable String resource) {
        if (isBlank(clientId)) {
            throw OAuthException.of(INVALID_REQUEST, "Missing client_id");
        }
        final OAuthClient client = clientService.findByClientId(clientId)
                .orElseThrow(() -> OAuthException.invalidClient("Unknown client"));
        final String effectiveRedirectUri = resolveRedirectUri(client, redirectUri);

        if (!RESPONSE_TYPE_CODE.equals(responseType)) {
            throw OAuthException.redirectable(UNSUPPORTED_RESPONSE_TYPE, "Only the code response type is supported");
        }
        if (isBlank(codeChallenge) || !OAuthTokens.isValidCodeChallenge(codeChallenge)) {
            throw OAuthException.redirectable(INVALID_REQUEST, "A valid PKCE code_challenge is required");
        }
        if (!CODE_CHALLENGE_METHOD_S256.equals(codeChallengeMethod)) {
            throw OAuthException.redirectable(INVALID_REQUEST, "Only the S256 code_challenge_method is supported");
        }
        if (!isBlank(resource) && !McpOAuthUrls.isMcpResource(resource)) {
            throw OAuthException.redirectable(INVALID_TARGET, "The requested resource is not the MCP endpoint");
        }
        return new AuthorizationRequest(client, effectiveRedirectUri, codeChallenge, state, isBlank(resource) ? null : resource);
    }

    /**
     * Builds the redirect for an error in an authorization request, if the client and redirect URI are known to be valid.
     */
    public Optional<String> errorRedirect(OAuthException e,
                                          @Nullable String clientId,
                                          @Nullable String redirectUri,
                                          @Nullable String state,
                                          String issuer) {
        if (!e.redirectable() || isBlank(clientId)) {
            return Optional.empty();
        }
        return clientService.findByClientId(clientId)
                .map(client -> {
                    try {
                        return resolveRedirectUri(client, redirectUri);
                    } catch (OAuthException ignored) {
                        return null;
                    }
                })
                .map(uri -> errorRedirectUri(uri, e.error(), e.description(), state, issuer));
    }

    /**
     * Records the user's consent and returns the redirect URI carrying the authorization code.
     */
    public String approve(AuthorizationRequest request, User user, String issuer) {
        final String code = OAuthTokens.generate();
        codeService.create(new OAuthAuthorizationCode(
                null,
                OAuthTokens.hash(code),
                request.client().clientId(),
                user.getId(),
                request.redirectUri(),
                request.codeChallenge(),
                request.resource(),
                Instant.now(clock).plus(AUTHORIZATION_CODE_TTL)
        ));
        auditEventSender.success(AuditActor.user(user), MCP_OAUTH_AUTHORIZATION_GRANT, clientContext(request.client()));

        final Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        params.put("iss", issuer);
        if (request.state() != null) {
            params.put("state", request.state());
        }
        return appendQueryParameters(request.redirectUri(), params);
    }

    public String deny(AuthorizationRequest request, User user, String issuer) {
        auditEventSender.success(AuditActor.user(user), MCP_OAUTH_AUTHORIZATION_DENY, clientContext(request.client()));
        return errorRedirectUri(request.redirectUri(), ACCESS_DENIED, "The user denied the request", request.state(), issuer);
    }

    public TokenResponse exchangeAuthorizationCode(@Nullable String clientId,
                                                   @Nullable String code,
                                                   @Nullable String redirectUri,
                                                   @Nullable String codeVerifier,
                                                   @Nullable String resource,
                                                   McpConfiguration config) {
        final OAuthClient client = loadClient(clientId);
        if (isBlank(code) || isBlank(codeVerifier)) {
            throw OAuthException.of(INVALID_REQUEST, "Missing code or code_verifier");
        }
        final OAuthAuthorizationCode authorizationCode = codeService.consume(OAuthTokens.hash(code))
                .orElseThrow(() -> OAuthException.of(INVALID_GRANT, "Invalid authorization code"));

        if (authorizationCode.expiresAt().isBefore(Instant.now(clock))) {
            throw OAuthException.of(INVALID_GRANT, "Authorization code expired");
        }
        if (!authorizationCode.clientId().equals(client.clientId())) {
            throw OAuthException.of(INVALID_GRANT, "Authorization code was issued to another client");
        }
        if (!isBlank(redirectUri) && !redirectUri.equals(authorizationCode.redirectUri())) {
            throw OAuthException.of(INVALID_GRANT, "redirect_uri does not match the authorization request");
        }
        if (!OAuthTokens.verifyPkce(codeVerifier, authorizationCode.codeChallenge())) {
            throw OAuthException.of(INVALID_GRANT, "PKCE verification failed");
        }
        final String effectiveResource = validateResource(resource, authorizationCode.resource());
        final User user = loadActiveUser(authorizationCode.userId());

        final TokenResponse response = issueTokens(client, user, effectiveResource, UUID.randomUUID().toString(), config);
        auditEventSender.success(AuditActor.user(user), MCP_OAUTH_TOKEN_ISSUE, clientContext(client));
        return response;
    }

    public TokenResponse refresh(@Nullable String clientId,
                                 @Nullable String refreshToken,
                                 @Nullable String resource,
                                 McpConfiguration config) {
        final OAuthClient client = loadClient(clientId);
        if (isBlank(refreshToken)) {
            throw OAuthException.of(INVALID_REQUEST, "Missing refresh_token");
        }
        final String tokenHash = OAuthTokens.hash(refreshToken);
        final Optional<OAuthRefreshToken> unused = refreshTokenService.markUsed(tokenHash);
        if (unused.isEmpty()) {
            refreshTokenService.findByHash(tokenHash).ifPresent(this::handleRefreshTokenReuse);
            throw OAuthException.of(INVALID_GRANT, "Invalid refresh token");
        }
        final OAuthRefreshToken token = unused.get();
        final Instant now = Instant.now(clock);

        if (!token.clientId().equals(client.clientId())) {
            throw OAuthException.of(INVALID_GRANT, "Refresh token was issued to another client");
        }
        if (token.expiresAt().isBefore(now)) {
            throw OAuthException.of(INVALID_GRANT, "Refresh token expired");
        }
        // The access token is gone before its expiry, so a user revoked it in the token management
        if (accessTokenService.loadById(token.accessTokenId()) == null && token.accessTokenExpiresAt().isAfter(now)) {
            LOG.info("Access token of OAuth client <{}> for user <{}> was revoked, revoking its refresh tokens.", token.clientId(), token.username());
            revokeTokens(token.username(), token.clientId());
            throw OAuthException.of(INVALID_GRANT, "Authorization was revoked");
        }
        final String effectiveResource = validateResource(resource, token.resource());
        final User user = loadActiveUser(token.userId());

        final TokenResponse response = issueTokens(client, user, effectiveResource, token.familyId(), config);
        auditEventSender.success(AuditActor.user(user), MCP_OAUTH_TOKEN_REFRESH, clientContext(client));
        return response;
    }

    /**
     * Token revocation (RFC 7009). Unknown tokens are ignored.
     */
    public void revoke(@Nullable String clientId, @Nullable String token) {
        if (isBlank(clientId) || isBlank(token)) {
            throw OAuthException.of(INVALID_REQUEST, "Missing client_id or token");
        }
        final Optional<OAuthRefreshToken> refreshToken = refreshTokenService.findByHash(OAuthTokens.hash(token));
        if (refreshToken.isPresent()) {
            if (refreshToken.get().clientId().equals(clientId)) {
                revokeTokens(refreshToken.get().username(), clientId);
                auditEventSender.success(AuditActor.user(refreshToken.get().username()), MCP_OAUTH_TOKEN_REVOKE, Map.of("client_id", clientId));
            }
            return;
        }
        final AccessToken accessToken = accessTokenService.load(token);
        if (accessToken != null && clientId.equals(accessToken.getOAuthClientId())) {
            accessTokenService.destroy(accessToken);
            auditEventSender.success(AuditActor.user(accessToken.getUserName()), MCP_OAUTH_TOKEN_REVOKE, Map.of("client_id", clientId));
        }
    }

    public List<Grant> grantsForUser(String username) {
        final Map<String, OAuthRefreshToken> latestByClient = refreshTokenService.findActiveByUsername(username).stream()
                .filter(token -> token.expiresAt().isAfter(Instant.now(clock)))
                .collect(Collectors.toMap(OAuthRefreshToken::clientId, Function.identity(),
                        (a, b) -> a.createdAt().isAfter(b.createdAt()) ? a : b));
        final Map<String, String> clientNames = clientService.findByClientIds(latestByClient.keySet()).stream()
                .collect(Collectors.toMap(OAuthClient::clientId, OAuthClient::clientName));

        return latestByClient.values().stream()
                .map(token -> new Grant(token.clientId(), clientNames.getOrDefault(token.clientId(), DEFAULT_CLIENT_NAME), token.createdAt(), token.expiresAt()))
                .sorted(Comparator.comparing(Grant::lastRefreshedAt).reversed())
                .toList();
    }

    public boolean revokeGrant(User user, String clientId) {
        final boolean revoked = revokeTokens(user.getName(), clientId) > 0;
        if (revoked) {
            auditEventSender.success(AuditActor.user(user), MCP_OAUTH_AUTHORIZATION_REVOKE, Map.of("client_id", clientId));
        }
        return revoked;
    }

    private TokenResponse issueTokens(OAuthClient client, User user, @Nullable String resource, String familyId, McpConfiguration config) {
        final Instant now = Instant.now(clock);
        final Duration accessTokenTtl = config.oauthAccessTokenTtl();
        final Instant refreshTokenExpiresAt = now.plus(config.oauthRefreshTokenTtl());

        final AccessToken accessToken = accessTokenService.create(
                user.getName(),
                truncate(f("MCP: %s", client.clientName()), MAX_CLIENT_NAME_LENGTH + 5),
                PeriodDuration.of(accessTokenTtl),
                McpRestResource.ACCESS_TOKEN_AUDIENCE,
                client.clientId()
        );
        final String refreshToken = OAuthTokens.generate();
        refreshTokenService.create(new OAuthRefreshToken(
                null,
                OAuthTokens.hash(refreshToken),
                familyId,
                client.clientId(),
                user.getId(),
                user.getName(),
                accessToken.getId(),
                now.plus(accessTokenTtl),
                resource,
                false,
                now,
                refreshTokenExpiresAt
        ));
        clientService.extendExpiry(client.clientId(), refreshTokenExpiresAt);

        return new TokenResponse(accessToken.getToken(), accessTokenTtl.toSeconds(), refreshToken);
    }

    private void handleRefreshTokenReuse(OAuthRefreshToken token) {
        LOG.warn("Refresh token reuse detected for OAuth client <{}> of user <{}>, revoking all of its tokens.", token.clientId(), token.username());
        revokeTokens(token.username(), token.clientId());
        auditEventSender.success(AuditActor.user(token.username()), MCP_OAUTH_REFRESH_TOKEN_REUSE, Map.of("client_id", token.clientId()));
    }

    private long revokeTokens(String username, String clientId) {
        final long refreshTokens = refreshTokenService.deleteByUsernameAndClientId(username, clientId);
        final int accessTokens = accessTokenService.deleteAllForOAuthClient(username, clientId);
        return refreshTokens + accessTokens;
    }

    private OAuthClient loadClient(@Nullable String clientId) {
        if (isBlank(clientId)) {
            throw OAuthException.invalidClient("Missing client_id");
        }
        return clientService.findByClientId(clientId).orElseThrow(() -> OAuthException.invalidClient("Unknown client"));
    }

    private User loadActiveUser(String userId) {
        final User user = userService.loadById(userId);
        if (user == null || user.getAccountStatus() != User.AccountStatus.ENABLED) {
            throw OAuthException.of(INVALID_GRANT, "User does not exist or is disabled");
        }
        return user;
    }

    @Nullable
    private static String validateResource(@Nullable String requested, @Nullable String granted) {
        if (isBlank(requested)) {
            return granted;
        }
        if (!McpOAuthUrls.isMcpResource(requested) || (granted != null && !granted.equals(requested))) {
            throw OAuthException.of(INVALID_TARGET, "The requested resource does not match the authorization");
        }
        return requested;
    }

    private static String resolveRedirectUri(OAuthClient client, @Nullable String redirectUri) {
        if (isBlank(redirectUri)) {
            if (client.redirectUris().size() == 1) {
                return client.redirectUris().getFirst();
            }
            throw OAuthException.of(INVALID_REQUEST, "Missing redirect_uri");
        }
        if (client.redirectUris().stream().noneMatch(registered -> RedirectUriPolicy.matches(registered, redirectUri))) {
            throw OAuthException.of(INVALID_REQUEST, "redirect_uri is not registered for this client");
        }
        return redirectUri;
    }

    private static String errorRedirectUri(String redirectUri, String error, @Nullable String description, @Nullable String state, String issuer) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("error", error);
        params.put("iss", issuer);
        if (description != null) {
            params.put("error_description", description);
        }
        if (state != null) {
            params.put("state", state);
        }
        return appendQueryParameters(redirectUri, params);
    }

    // UriBuilder would interpret braces in client-provided values as URI templates
    static String appendQueryParameters(String uri, Map<String, String> params) {
        final String query = params.entrySet().stream()
                .map(e -> f("%s=%s", e.getKey(), URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)))
                .collect(Collectors.joining("&"));
        return f("%s%s%s", uri, uri.contains("?") ? "&" : "?", query);
    }

    private static String sanitizeClientName(@Nullable String clientName) {
        if (isBlank(clientName)) {
            return DEFAULT_CLIENT_NAME;
        }
        final String printable = clientName.replaceAll("\\p{Cntrl}", "").strip();
        return printable.isEmpty() ? DEFAULT_CLIENT_NAME : truncate(printable, MAX_CLIENT_NAME_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static Map<String, Object> clientContext(OAuthClient client) {
        return Map.of("client_id", client.clientId(), "client_name", client.clientName());
    }
}
