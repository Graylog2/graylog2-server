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

import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.server.McpRestResource;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.threeten.extra.PeriodDuration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpOAuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final String ISSUER = "https://graylog.example.com";
    private static final String REDIRECT_URI = "http://127.0.0.1:33418/callback";
    private static final String VERIFIER = "dBjftJeZ4CVP-mJ92K9yY6qZ9sEb7rfo4M5fdRFOrqA";
    private static final String CHALLENGE = "y369XJCBGNH-E16nEZoxIbr002YWs9OIKuuf5ExCbFg";
    private static final String RESOURCE = "https://graylog.example.com/api/mcp";
    private static final McpConfiguration CONFIG = McpConfiguration.create(true, false, false);
    private static final OAuthClient CLIENT = new OAuthClient("id", "mcp-client", "Claude Code", null,
            List.of(REDIRECT_URI), NOW.minusSeconds(60), NOW.plus(Duration.ofDays(1)));

    private OAuthClientService clientService;
    private OAuthAuthorizationCodeService codeService;
    private OAuthRefreshTokenService refreshTokenService;
    private AccessTokenService accessTokenService;
    private UserService userService;
    private McpOAuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        clientService = mock(OAuthClientService.class);
        codeService = mock(OAuthAuthorizationCodeService.class);
        refreshTokenService = mock(OAuthRefreshTokenService.class);
        accessTokenService = mock(AccessTokenService.class);
        userService = mock(UserService.class);
        service = new McpOAuthService(clientService, codeService, refreshTokenService, accessTokenService, userService,
                mock(AuditEventSender.class), Clock.fixed(NOW, ZoneOffset.UTC));

        user = mock(User.class);
        when(user.getId()).thenReturn("user-id");
        when(user.getName()).thenReturn("jane");
        when(user.getAccountStatus()).thenReturn(User.AccountStatus.ENABLED);
        when(userService.loadById("user-id")).thenReturn(user);
        when(clientService.findByClientId(CLIENT.clientId())).thenReturn(Optional.of(CLIENT));
    }

    @Test
    void registersPublicClientWithSanitizedName() {
        final OAuthClient client = service.registerClient(new McpOAuthService.ClientRegistrationRequest(
                List.of(REDIRECT_URI), "  Claude\u0000 Code ", null, List.of("authorization_code", "refresh_token"), List.of("code")), CONFIG);

        assertThat(client.clientId()).startsWith("mcp-");
        assertThat(client.clientName()).isEqualTo("Claude Code");
        assertThat(client.expiresAt()).isEqualTo(NOW.plus(McpOAuthService.UNUSED_CLIENT_TTL));
        verify(clientService).create(client);
    }

    @Test
    void rejectsRegistrationWithForeignRedirectUri() {
        assertThatThrownBy(() -> service.registerClient(new McpOAuthService.ClientRegistrationRequest(
                List.of("https://evil.example.com/callback"), "Evil", null, null, null), CONFIG))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.error()).isEqualTo(OAuthException.INVALID_REDIRECT_URI));
        verify(clientService, never()).create(any());
    }

    @Test
    void rejectsRegistrationWithUnsupportedGrantType() {
        assertThatThrownBy(() -> service.registerClient(new McpOAuthService.ClientRegistrationRequest(
                List.of(REDIRECT_URI), null, null, List.of("client_credentials"), null), CONFIG))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.error()).isEqualTo(OAuthException.INVALID_CLIENT_METADATA));
    }

    @Test
    void unknownClientErrorsAreNotRedirected() {
        assertThatThrownBy(() -> validate("unknown", REDIRECT_URI, CHALLENGE, "S256"))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.redirectable()).isFalse());
    }

    @Test
    void unregisteredRedirectUriErrorsAreNotRedirected() {
        assertThatThrownBy(() -> validate(CLIENT.clientId(), "http://127.0.0.1:33418/other", CHALLENGE, "S256"))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.redirectable()).isFalse());
    }

    @Test
    void requiresS256Pkce() {
        assertThatThrownBy(() -> validate(CLIENT.clientId(), REDIRECT_URI, null, null))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.redirectable()).isTrue());
        assertThatThrownBy(() -> validate(CLIENT.clientId(), REDIRECT_URI, CHALLENGE, "plain"))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.redirectable()).isTrue());
    }

    @Test
    void allowsDifferentLoopbackPort() {
        final McpOAuthService.AuthorizationRequest request = validate(CLIENT.clientId(), "http://127.0.0.1:50000/callback", CHALLENGE, "S256");

        assertThat(request.redirectUri()).isEqualTo("http://127.0.0.1:50000/callback");
    }

    @Test
    void redirectsRedirectableErrorsWithState() {
        final OAuthException error = OAuthException.redirectable(OAuthException.INVALID_REQUEST, "PKCE {required}");

        assertThat(service.errorRedirect(error, CLIENT.clientId(), REDIRECT_URI, "a b", ISSUER))
                .contains("http://127.0.0.1:33418/callback?error=invalid_request&iss=https%3A%2F%2Fgraylog.example.com"
                        + "&error_description=PKCE+%7Brequired%7D&state=a+b");
    }

    @Test
    void issuesAudienceRestrictedTokensForValidCode() {
        final String code = approveAndCaptureCode();
        final AccessToken accessToken = stubAccessTokenCreation();

        final McpOAuthService.TokenResponse response = service.exchangeAuthorizationCode(CLIENT.clientId(), code,
                REDIRECT_URI, VERIFIER, RESOURCE, CONFIG);

        assertThat(response.accessToken()).isEqualTo(accessToken.getToken());
        assertThat(response.expiresIn()).isEqualTo(CONFIG.oauthAccessTokenTtl().toSeconds());
        assertThat(response.refreshToken()).isNotBlank();
        verify(accessTokenService).create(eq("jane"), eq("MCP: Claude Code"), eq(PeriodDuration.of(CONFIG.oauthAccessTokenTtl())),
                eq(McpRestResource.ACCESS_TOKEN_AUDIENCE), eq(CLIENT.clientId()));

        final ArgumentCaptor<OAuthRefreshToken> refreshToken = ArgumentCaptor.forClass(OAuthRefreshToken.class);
        verify(refreshTokenService).create(refreshToken.capture());
        assertThat(refreshToken.getValue().tokenHash()).isEqualTo(OAuthTokens.hash(response.refreshToken()));
        assertThat(refreshToken.getValue().accessTokenId()).isEqualTo("access-token-id");
        assertThat(refreshToken.getValue().expiresAt()).isEqualTo(NOW.plus(CONFIG.oauthRefreshTokenTtl()));
        verify(clientService).extendExpiry(CLIENT.clientId(), NOW.plus(CONFIG.oauthRefreshTokenTtl()));
    }

    @Test
    void rejectsCodeWithWrongVerifier() {
        final String code = approveAndCaptureCode();

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(CLIENT.clientId(), code, REDIRECT_URI,
                VERIFIER.replace('d', 'e'), null, CONFIG))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.error()).isEqualTo(OAuthException.INVALID_GRANT));
        verify(accessTokenService, never()).create(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void rejectsCodeForOtherResource() {
        final String code = approveAndCaptureCode();

        assertThatThrownBy(() -> service.exchangeAuthorizationCode(CLIENT.clientId(), code, REDIRECT_URI, VERIFIER,
                "https://other.example.com/api/mcp", CONFIG))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.error()).isEqualTo(OAuthException.INVALID_TARGET));
    }

    @Test
    void rotatesRefreshTokenWithinFamily() {
        final OAuthRefreshToken existing = refreshToken(NOW.plusSeconds(60));
        when(refreshTokenService.markUsed(OAuthTokens.hash("refresh"))).thenReturn(Optional.of(existing));
        when(accessTokenService.loadById("old-access-token")).thenReturn(mock(AccessToken.class));
        stubAccessTokenCreation();

        final McpOAuthService.TokenResponse response = service.refresh(CLIENT.clientId(), "refresh", null, CONFIG);

        final ArgumentCaptor<OAuthRefreshToken> rotated = ArgumentCaptor.forClass(OAuthRefreshToken.class);
        verify(refreshTokenService).create(rotated.capture());
        assertThat(rotated.getValue().familyId()).isEqualTo(existing.familyId());
        assertThat(rotated.getValue().tokenHash()).isEqualTo(OAuthTokens.hash(response.refreshToken()));
    }

    @Test
    void revokesAllTokensOnRefreshTokenReuse() {
        when(refreshTokenService.markUsed(OAuthTokens.hash("refresh"))).thenReturn(Optional.empty());
        when(refreshTokenService.findByHash(OAuthTokens.hash("refresh"))).thenReturn(Optional.of(refreshToken(NOW.plusSeconds(60))));

        assertThatThrownBy(() -> service.refresh(CLIENT.clientId(), "refresh", null, CONFIG))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.error()).isEqualTo(OAuthException.INVALID_GRANT));
        verify(refreshTokenService).deleteByUsernameAndClientId("jane", CLIENT.clientId());
        verify(accessTokenService).deleteAllForOAuthClient("jane", CLIENT.clientId());
    }

    @Test
    void refusesRefreshAfterAccessTokenWasRevoked() {
        when(refreshTokenService.markUsed(OAuthTokens.hash("refresh"))).thenReturn(Optional.of(refreshToken(NOW.plusSeconds(60))));
        when(accessTokenService.loadById("old-access-token")).thenReturn(null);

        assertThatThrownBy(() -> service.refresh(CLIENT.clientId(), "refresh", null, CONFIG))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.error()).isEqualTo(OAuthException.INVALID_GRANT));
        verify(refreshTokenService).deleteByUsernameAndClientId("jane", CLIENT.clientId());
    }

    @Test
    void allowsRefreshAfterAccessTokenExpired() {
        when(refreshTokenService.markUsed(OAuthTokens.hash("refresh"))).thenReturn(Optional.of(refreshToken(NOW.minusSeconds(60))));
        when(accessTokenService.loadById("old-access-token")).thenReturn(null);
        stubAccessTokenCreation();

        assertThat(service.refresh(CLIENT.clientId(), "refresh", null, CONFIG).accessToken()).isEqualTo("access-token");
    }

    @Test
    void rejectsRefreshForDisabledUser() {
        when(refreshTokenService.markUsed(OAuthTokens.hash("refresh"))).thenReturn(Optional.of(refreshToken(NOW.minusSeconds(60))));
        when(user.getAccountStatus()).thenReturn(User.AccountStatus.DISABLED);

        assertThatThrownBy(() -> service.refresh(CLIENT.clientId(), "refresh", null, CONFIG))
                .isInstanceOfSatisfying(OAuthException.class, e -> assertThat(e.error()).isEqualTo(OAuthException.INVALID_GRANT));
    }

    @Test
    void appendsToExistingQuery() {
        assertThat(McpOAuthService.appendQueryParameters("http://127.0.0.1/cb?x=1", Map.of("code", "a/b")))
                .isEqualTo("http://127.0.0.1/cb?x=1&code=a%2Fb");
    }

    private McpOAuthService.AuthorizationRequest validate(String clientId, String redirectUri, String challenge, String method) {
        return service.validateAuthorizationRequest(clientId, redirectUri, "code", challenge, method, "state", RESOURCE);
    }

    private String approveAndCaptureCode() {
        final String redirect = service.approve(validate(CLIENT.clientId(), REDIRECT_URI, CHALLENGE, "S256"), user, ISSUER);
        final String code = redirect.replaceAll(".*[?&]code=([^&]+).*", "$1");

        final ArgumentCaptor<OAuthAuthorizationCode> stored = ArgumentCaptor.forClass(OAuthAuthorizationCode.class);
        verify(codeService).create(stored.capture());
        assertThat(stored.getValue().codeHash()).isEqualTo(OAuthTokens.hash(code));
        assertThat(redirect).contains("state=state").contains("iss=");
        when(codeService.consume(OAuthTokens.hash(code))).thenReturn(Optional.of(stored.getValue()));
        return code;
    }

    private AccessToken stubAccessTokenCreation() {
        final AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getId()).thenReturn("access-token-id");
        when(accessToken.getToken()).thenReturn("access-token");
        when(accessTokenService.create(anyString(), anyString(), any(), anyString(), any())).thenReturn(accessToken);
        return accessToken;
    }

    private OAuthRefreshToken refreshToken(Instant accessTokenExpiresAt) {
        return new OAuthRefreshToken("rt-id", OAuthTokens.hash("refresh"), "family", CLIENT.clientId(), "user-id", "jane",
                "old-access-token", accessTokenExpiresAt, null, false, NOW.minusSeconds(3600), NOW.plus(Duration.ofDays(10)));
    }
}
