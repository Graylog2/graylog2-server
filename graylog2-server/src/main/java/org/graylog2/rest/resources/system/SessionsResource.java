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
package org.graylog2.rest.resources.system;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.glassfish.grizzly.http.server.Request;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.RestTools;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;
import org.graylog2.rest.models.system.sessions.responses.SessionResponseFactory;
import org.graylog2.rest.models.system.sessions.responses.SessionValidationResponse;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.ActorAwareAuthenticationToken;
import org.graylog2.shared.security.ActorAwareAuthenticationTokenFactory;
import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
import org.graylog2.shared.security.SessionCreator;
import org.graylog2.shared.security.ShiroAuthenticationFilter;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.shared.users.UserService;
import org.graylog2.utilities.IpSubnet;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Path("/system/sessions")
@PublicCloudAPI
@Tag(name = "System/Sessions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionsResource extends RestResource {
    private final DefaultSecurityManager securityManager;
    private final ShiroAuthenticationFilter authenticationFilter;
    private final Set<IpSubnet> trustedSubnets;
    private final Request grizzlyRequest;
    private final SessionCreator sessionCreator;
    private final ActorAwareAuthenticationTokenFactory tokenFactory;
    private final CookieFactory cookieFactory;

    private static final String USERNAME = "username";

    @Inject
    public SessionsResource(UserService userService,
                            DefaultSecurityManager securityManager,
                            ShiroAuthenticationFilter authenticationFilter,
                            @Named("trusted_proxies") Set<IpSubnet> trustedSubnets,
                            @Context Request grizzlyRequest,
                            SessionCreator sessionCreator,
                            ActorAwareAuthenticationTokenFactory tokenFactory,
                            CookieFactory cookieFactory) {
        this.cookieFactory = cookieFactory;
        this.userService = userService;
        this.securityManager = securityManager;
        this.authenticationFilter = authenticationFilter;
        this.trustedSubnets = trustedSubnets;
        this.grizzlyRequest = grizzlyRequest;
        this.sessionCreator = sessionCreator;
        this.tokenFactory = tokenFactory;
    }

    @POST
    @Operation(summary = "Create a new session",
                  description = "This request creates a new session for a user or reactivates an existing session: the equivalent of logging in.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session created successfully",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class)))
    })
    @NoAuditEvent("dispatches audit events in the method body")
    public Response newSession(@Context ContainerRequestContext requestContext,
                               @Parameter(name = "Login request", description = "Credentials. The default " +
                                       "implementation requires presence of two properties: 'username' and " +
                                       "'password'. However a plugin may customize which kind of credentials " +
                                       "are accepted and therefore expect different properties.",
                                         required = true)
                               @NotNull JsonNode createRequest) {

        rejectServiceAccount(createRequest);

        final ActorAwareAuthenticationToken authToken;
        try {
            authToken = tokenFactory.forRequestBody(createRequest);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        final String host = RestTools.getRemoteAddrFromRequest(grizzlyRequest, trustedSubnets);

        try {
            // Always create a brand-new session for an authentication attempt by ignoring any previous session ID.
            // This avoids a potential session fixation attack. (GHSA-3xf8-g8gr-g7rh)
            final Optional<Session> session = sessionCreator.login(host, authToken);
            if (session.isPresent()) {
                final SessionResponse response = SessionResponseFactory.forSession(session.get());
                return Response.ok()
                        .entity(response)
                        .cookie(cookieFactory.createAuthenticationCookie(session.get(), requestContext))
                        .build();
            } else {
                throw new NotAuthorizedException("Invalid credentials.", "Basic realm=\"Graylog Server session\"");
            }
        } catch (AuthenticationServiceUnavailableException e) {
            throw new ServiceUnavailableException("Authentication service unavailable");
        }
    }

    private void rejectServiceAccount(JsonNode createRequest) {
        if (createRequest.has(USERNAME)) {
            final User user = userService.load(createRequest.get(USERNAME).asText());
            if ((user != null) && user.isServiceAccount()) {
                throw new BadRequestException("Cannot login with service account " + user.getName());
            }
        }
    }

    @GET
    @Operation(summary = "Validate an existing session",
                  description = "Checks the session with the given ID: returns http status 204 (No Content) if session is valid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session validation result",
                    content = @Content(schema = @Schema(implementation = SessionValidationResponse.class)))
    })
    public Response validateSession(@Context ContainerRequestContext requestContext) {
        try {
            this.authenticationFilter.filter(requestContext);
        } catch (NotAuthorizedException | LockedAccountException | IOException e) {
            return Response.ok(SessionValidationResponse.invalid())
                    .cookie(cookieFactory.deleteAuthenticationCookie(requestContext))
                    .build();
        }
        final Subject subject = getSubject();
        if (!subject.isAuthenticated()) {
            return Response.ok(SessionValidationResponse.invalid())
                    .cookie(cookieFactory.deleteAuthenticationCookie(requestContext))
                    .build();
        }

        final Optional<Session> optionalSession = Optional.ofNullable(retrieveOrCreateSession(subject));

        final User user = getCurrentUser();
        return optionalSession.map(session -> Response.ok(
                        SessionValidationResponse.validWithNewSession(
                                String.valueOf(session.getId()),
                                String.valueOf(user.getName())
                        ))
                .cookie(cookieFactory.createAuthenticationCookie(session, requestContext))
                .build()).orElseGet(() -> Response.ok(SessionValidationResponse.authenticatedWithNoSession(user.getName()))
                .cookie(cookieFactory.deleteAuthenticationCookie(requestContext))
                .build());
    }

    private Session retrieveOrCreateSession(Subject subject) {
        final Session potentialSession = subject.getSession(false);
        if (needToCreateNewSession(potentialSession)) {
            // There's no valid session, but the authenticator would like us to create one.
            // This is the "Trusted Header Authentication" scenario, where the browser performs this request to check if a
            // session exists, with a trusted header identifying the user. The authentication filter will authenticate the
            // user based on the trusted header and request a session to be created transparently. The UI will take the
            // session information from the response to perform subsequent requests to the backend using this session.
            return sessionCreator.createForSubject(subject);
        }

        return potentialSession;
    }

    private boolean needToCreateNewSession(Session potentialSession) {
        return potentialSession == null && ShiroSecurityContext.isSessionCreationRequested();
    }

    @DELETE
    @Operation(summary = "Terminate an existing session", description = "Destroys the session with the given ID: the equivalent of logging out.")
    @Path("/{sessionId}")
    @RequiresAuthentication
    @Deprecated
    @AuditEvent(type = AuditEventTypes.SESSION_DELETE)
    public Response terminateSessionWithId(@Parameter(name = "sessionId", required = true) @PathParam("sessionId") String sessionId,
                                           @Context ContainerRequestContext requestContext) {
        final Subject subject = getSubject();
        securityManager.logout(subject);

        return Response.ok()
                .cookie(cookieFactory.deleteAuthenticationCookie(requestContext))
                .build();
    }

    @DELETE
    @Operation(summary = "Terminate an existing session", description = "Destroys the session with the given ID: the equivalent of logging out.")
    @RequiresAuthentication
    @AuditEvent(type = AuditEventTypes.SESSION_DELETE)
    public Response terminateSession(@Context ContainerRequestContext requestContext) {
        final Subject subject = getSubject();
        securityManager.logout(subject);

        return Response.ok()
                .cookie(cookieFactory.deleteAuthenticationCookie(requestContext))
                .build();
    }
}
