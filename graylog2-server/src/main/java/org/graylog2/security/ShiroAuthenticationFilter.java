/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.LockedAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

/**
 * @author Kay Roepke <kay@torch.sh>
 */
public class ShiroAuthenticationFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ShiroAuthenticationFilter.class);

    public ShiroAuthenticationFilter() {

    }
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final SecurityContext securityContext = requestContext.getSecurityContext();
        if (!(securityContext instanceof ShiroSecurityContext)) {
            return;
        }
        final ShiroSecurityContext context = (ShiroSecurityContext) securityContext;
        LOG.trace("Authenticating... {}", context.getSubject());
        if (!context.getSubject().isAuthenticated()) {
            try {
                LOG.trace("Logging in {}", context.getSubject());
                context.loginSubject();

            } catch (LockedAccountException e) {
                LOG.debug("Unable to authenticate user, account is locked.", e);
                throw new NotAuthorizedException(e, "Basic realm=\"Graylog2 Server\"");
            } catch (AuthenticationException e) {
                LOG.debug("Unable to authenticate user.", e);
                throw new NotAuthorizedException(e, "Basic realm=\"Graylog2 Server\"");
            }
        }
    }
}
