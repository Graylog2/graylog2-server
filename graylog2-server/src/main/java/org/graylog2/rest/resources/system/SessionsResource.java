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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.glassfish.grizzly.http.server.Request;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.RestTools;
import org.graylog2.rest.models.system.sessions.responses.SessionResponseFactory;
import org.graylog2.rest.models.system.sessions.responses.SessionValidationResponse;
import org.graylog2.security.headerauth.HTTPHeaderAuthConfig;
import org.graylog2.security.realm.HTTPHeaderAuthenticationRealm;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.ActorAwareAuthenticationToken;
import org.graylog2.shared.security.ActorAwareAuthenticationTokenFactory;
import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
import org.graylog2.shared.security.SessionCreator;
import org.graylog2.shared.security.ShiroAuthenticationFilter;
import org.graylog2.shared.security.ShiroRequestHeadersBinder;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.shared.users.UserService;
import org.graylog2.utilities.IpSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Path("/system/sessions")
@Api(value = "System/Sessions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SessionsResource.class);

    private final DefaultSecurityManager securityManager;
    private final ShiroAuthenticationFilter authenticationFilter;
    private final Set<IpSubnet> trustedSubnets;
    private final Request grizzlyRequest;
    private final SessionCreator sessionCreator;
    private final ActorAwareAuthenticationTokenFactory tokenFactory;
    private final SessionResponseFactory sessionResponseFactory;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public SessionsResource(UserService userService,
                            DefaultSecurityManager securityManager,
                            ShiroAuthenticationFilter authenticationFilter,
                            @Named("trusted_proxies") Set<IpSubnet> trustedSubnets,
                            @Context Request grizzlyRequest,
                            SessionCreator sessionCreator,
                            ActorAwareAuthenticationTokenFactory tokenFactory,
                            SessionResponseFactory sessionResponseFactory,
                            ClusterConfigService clusterConfigService) {
        this.userService = userService;
        this.securityManager = securityManager;
        this.authenticationFilter = authenticationFilter;
        this.trustedSubnets = trustedSubnets;
        this.grizzlyRequest = grizzlyRequest;
        this.sessionCreator = sessionCreator;
        this.tokenFactory = tokenFactory;
        this.sessionResponseFactory = sessionResponseFactory;
        this.clusterConfigService = clusterConfigService;
    }

    @POST
    @ApiOperation(value = "Create a new session", notes = "This request creates a new session for a user or " +
            "reactivates an existing session: the equivalent of logging in.")
    @NoAuditEvent("dispatches audit events in the method body")
    public JsonNode newSession(@Context ContainerRequestContext requestContext,
                                      @ApiParam(name = "Login request", value = "Credentials. The default " +
                                              "implementation requires presence of two properties: 'username' and " +
                                              "'password'. However a plugin may customize which kind of credentials " +
                                              "are accepted and therefore expect different properties.",
                                              required = true)
                                      @NotNull JsonNode createRequest) {

        final SecurityContext securityContext = requestContext.getSecurityContext();
        if (!(securityContext instanceof ShiroSecurityContext)) {
            throw new InternalServerErrorException("Unsupported SecurityContext class, this is a bug!");
        }
        final ShiroSecurityContext shiroSecurityContext = (ShiroSecurityContext) securityContext;

        final ActorAwareAuthenticationToken authToken;
        try {
            authToken = tokenFactory.forRequestBody(createRequest);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        // we treat the BASIC auth username as the sessionid
        final String sessionId = shiroSecurityContext.getUsername();
        final String host = RestTools.getRemoteAddrFromRequest(grizzlyRequest, trustedSubnets);

        try {
            Optional<Session> session = sessionCreator.create(sessionId, host, authToken);
            if (session.isPresent()) {
                return sessionResponseFactory.forSession(session.get());
            } else {
                throw new NotAuthorizedException("Invalid credentials.", "Basic realm=\"Graylog Server session\"");
            }
        } catch (AuthenticationServiceUnavailableException e) {
            throw new ServiceUnavailableException("Authentication service unavailable");
        }
    }

    @GET
    @ApiOperation(value = "Validate an existing session",
        notes = "Checks the session with the given ID: returns http status 204 (No Content) if session is valid.",
        code = 204
    )
    public SessionValidationResponse validateSession(@Context ContainerRequestContext requestContext) {
        try {
            this.authenticationFilter.filter(requestContext);
        } catch (NotAuthorizedException | LockedAccountException | IOException e) {
            return SessionValidationResponse.invalid();
        }
        final Subject subject = getSubject();
        if (!subject.isAuthenticated()) {
            return SessionValidationResponse.invalid();
        }

        // There's no valid session, but the authenticator would like us to create one.
        // This is the "Trusted Header Authentication" scenario, where the browser performs this request to check if a
        // session exists, with a trusted header identifying the user. The authentication filter will authenticate the
        // user based on the trusted header and request a session to be created transparently. The UI will take the
        // session information from the response to perform subsequent requests to the backend using this session.
        if (subject.getSession(false) == null && ShiroSecurityContext.isSessionCreationRequested()) {
            final Session session = subject.getSession();

            final String userId = subject.getPrincipal().toString();
            final User user = userService.loadById(userId);
            if (user == null) {
                throw new InternalServerErrorException("Unable to load user with ID <" + userId + ">.");
            }

            session.setAttribute("username", user.getName());

            final HTTPHeaderAuthConfig httpHeaderConfig = loadHTTPHeaderConfig();
            final Optional<String> usernameHeader = ShiroRequestHeadersBinder.getHeaderFromThreadContext(httpHeaderConfig.usernameHeader());
            if (httpHeaderConfig.enabled() && usernameHeader.isPresent()) {
                session.setAttribute(HTTPHeaderAuthenticationRealm.SESSION_AUTH_HEADER, usernameHeader.get());
            }

            LOG.debug("Session created {}", session.getId());
            session.touch();
            // save subject in session, otherwise we can't get the username back in subsequent requests.
            ((DefaultSecurityManager) SecurityUtils.getSecurityManager()).getSubjectDAO().save(subject);

            return SessionValidationResponse.validWithNewSession(String.valueOf(session.getId()),
                                                                 String.valueOf(user.getName()));
        }
        return SessionValidationResponse.valid();
    }

    @DELETE
    @ApiOperation(value = "Terminate an existing session", notes = "Destroys the session with the given ID: the equivalent of logging out.")
    @Path("/{sessionId}")
    @RequiresAuthentication
    @AuditEvent(type = AuditEventTypes.SESSION_DELETE)
    public void terminateSession(@ApiParam(name = "sessionId", required = true) @PathParam("sessionId") String sessionId) {
        final Subject subject = getSubject();
        securityManager.logout(subject);
    }

    private HTTPHeaderAuthConfig loadHTTPHeaderConfig() {
        return clusterConfigService.getOrDefault(HTTPHeaderAuthConfig.class, HTTPHeaderAuthConfig.createDisabled());
    }
}
