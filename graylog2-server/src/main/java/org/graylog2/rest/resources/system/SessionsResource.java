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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.rest.models.system.sessions.requests.SessionCreateRequest;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Path("/system/sessions")
@Api(value = "System/Sessions", description = "Login for interactive user sessions")
public class SessionsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SessionsResource.class);

    private final UserService userService;
    private final DefaultSecurityManager securityManager;

    @Inject
    public SessionsResource(UserService userService,
                            DefaultSecurityManager securityManager) {
        this.userService = userService;
        this.securityManager = securityManager;
    }

    @POST
    @ApiOperation(value = "Create a new session", notes = "This request creates a new session for a user or reactivates an existing session: the equivalent of logging in.")
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
        // pretend that we had session id before
        Serializable id = null;
        if (sessionId != null && !sessionId.isEmpty()) {
            id = sessionId;
        }
        final Subject subject = new Subject.Builder().sessionId(id).buildSubject();
        ThreadContext.bind(subject);

        try {
            subject.login(new UsernamePasswordToken(createRequest.username(), createRequest.password()));
            final User user = userService.load(createRequest.username());
            if (user != null) {
                long timeoutInMillis = user.getSessionTimeoutMs();
                subject.getSession().setTimeout(timeoutInMillis);
            } else {
                // set a sane default. really we should be able to load the user from above.
                subject.getSession().setTimeout(TimeUnit.HOURS.toMillis(8));
            }
            subject.getSession().touch();

            // save subject in session, otherwise we can't get the username back in subsequent requests.
            ((DefaultSecurityManager) SecurityUtils.getSecurityManager()).getSubjectDAO().save(subject);

        } catch (AuthenticationException e) {
            LOG.warn("Unable to log in user " + createRequest.username(), e);
        } catch (UnknownSessionException e) {
            subject.logout();
        }
        if (subject.isAuthenticated()) {
            final org.apache.shiro.session.Session session = subject.getSession();
            id = session.getId();
            // TODO is the validUntil attribute even used by anyone yet?
            return SessionResponse.create(new DateTime(session.getLastAccessTime(), DateTimeZone.UTC).plus(session.getTimeout()).toDate(),
                    id.toString());
        }
        throw new NotAuthorizedException("Invalid username or password", "Basic realm=\"Graylog Server session\"");
    }

    @DELETE
    @ApiOperation(value = "Terminate an existing session", notes = "Destroys the session with the given ID: the equivalent of logging out.")
    @Path("/{sessionId}")
    @RequiresAuthentication
    public void terminateSession(@ApiParam(name = "sessionId", required = true) @PathParam("sessionId") String sessionId) {
        final Subject subject = getSubject();
        securityManager.logout(subject);
    }
}
