/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.glassfish.grizzly.http.server.Request;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.rest.RestTools;
import org.graylog2.rest.models.system.sessions.requests.SessionCreateRequest;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;
import org.graylog2.rest.models.system.sessions.responses.SessionValidationResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.SessionCreator;
import org.graylog2.shared.security.ShiroAuthenticationFilter;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.shared.users.UserService;
import org.graylog2.utilities.IpSubnet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.graylog2.audit.AuditEventTypes.SESSION_CREATE;

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
    private final AuditEventSender auditEventSender;

    @Inject
    public SessionsResource(UserService userService,
                            DefaultSecurityManager securityManager,
                            ShiroAuthenticationFilter authenticationFilter,
                            @Named("trusted_proxies") Set<IpSubnet> trustedSubnets,
                            @Context Request grizzlyRequest, SessionCreator sessionCreator,
                            AuditEventSender auditEventSender) {
        this.auditEventSender = auditEventSender;
        this.userService = userService;
        this.securityManager = securityManager;
        this.authenticationFilter = authenticationFilter;
        this.trustedSubnets = trustedSubnets;
        this.grizzlyRequest = grizzlyRequest;
        this.sessionCreator = sessionCreator;
    }

    @POST
    @ApiOperation(value = "Create a new session", notes = "This request creates a new session for a user or reactivates an existing session: the equivalent of logging in.")
    @NoAuditEvent("dispatches audit events in the method body")
    public SessionResponse newSession(@Context ContainerRequestContext requestContext,
                                      @ApiParam(name = "Login request", value = "Username and credentials", required = true)
                                      @Valid @NotNull SessionCreateRequest createRequest) {

        final SecurityContext securityContext = requestContext.getSecurityContext();
        if (!(securityContext instanceof ShiroSecurityContext)) {
            throw new InternalServerErrorException("Unsupported SecurityContext class, this is a bug!");
        }
        final ShiroSecurityContext shiroSecurityContext = (ShiroSecurityContext) securityContext;

        // we treat the BASIC auth username as the sessionid
        final String sessionId = shiroSecurityContext.getUsername();
        final String host = RestTools.getRemoteAddrFromRequest(grizzlyRequest, trustedSubnets);
        final String username = createRequest.username();
        final UsernamePasswordToken authToken = new UsernamePasswordToken(username, createRequest.password());

        try {
            Session session = sessionCreator.create(sessionId, host, authToken);
            final Map<String, Object> auditEventContext = ImmutableMap.of(
                    "session_id", session.getId(),
                    "remote_address", host
            );
            auditEventSender.success(AuditActor.user(username), SESSION_CREATE, auditEventContext);
            return SessionResponse.create(new DateTime(session.getLastAccessTime(), DateTimeZone.UTC).plus(session.getTimeout()).toDate(),
                    session.getId().toString());
        } catch (AuthenticationException e) {
            LOG.info("Invalid username or password for user \"{}\"", username);
            final Map<String, Object> auditEventContext = ImmutableMap.of(
                    "remote_address", host
            );
            auditEventSender.failure(AuditActor.user(username), SESSION_CREATE, auditEventContext);
            throw new NotAuthorizedException("Invalid username or password", "Basic realm=\"Graylog Server session\"");
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

        // there's no valid session, but the authenticator would like us to create one
        if (subject.getSession(false) == null && ShiroSecurityContext.isSessionCreationRequested()) {
            final Session session = subject.getSession();
            LOG.debug("Session created {}", session.getId());
            session.touch();
            // save subject in session, otherwise we can't get the username back in subsequent requests.
            ((DefaultSecurityManager) SecurityUtils.getSecurityManager()).getSubjectDAO().save(subject);

            return SessionValidationResponse.validWithNewSession(String.valueOf(session.getId()),
                                                                 String.valueOf(subject.getPrincipal()));
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
}
