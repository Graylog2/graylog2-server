package org.graylog2.security;

import org.apache.shiro.authc.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

public class ShiroAuthenticationFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ShiroAuthenticationFilter.class);

    public ShiroAuthenticationFilter() {

    }
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final SecurityContext securityContext = requestContext.getSecurityContext();
        if (!(securityContext instanceof ShiroSecurityContext)) {
            return;
        }
        final ShiroSecurityContext context = (ShiroSecurityContext) securityContext;
        log.trace("Authenticating... {}", context.getSubject());
        if (!context.getSubject().isAuthenticated()) {
            try {
                log.trace("Logging in {}", context.getSubject());
                context.loginSubject();
            } catch (AuthenticationException e) {
                log.debug("Unable to authenticate user.", e);
                throw new NotAuthorizedException(e, "Basic", "Graylog2 Server");
            }
        }
    }
}
