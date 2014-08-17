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

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;

public class ShiroSecurityContextFactory implements SecurityContextFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ShiroSecurityContextFactory.class);
    private final DefaultSecurityManager sm;

    @Inject
    public ShiroSecurityContextFactory(DefaultSecurityManager sm) {
        this.sm = sm;
    }

    @Override
    public SecurityContext create(String userName, String credential, boolean isSecure, String authcScheme, String host) {

        AuthenticationToken authToken;
        if (credential == null) {
            authToken = new UsernamePasswordToken(userName, credential, host);
        } else {
            if (credential.equalsIgnoreCase("session")) {
                authToken = new SessionIdToken(userName, host);
            } else if (credential.equalsIgnoreCase("token")) {
                authToken = new AccessTokenAuthToken(userName, host);
            } else {
                authToken = new UsernamePasswordToken(userName, credential, host);
            }
        }

        return new ShiroSecurityContext(
                new Subject.Builder(sm).host(host).sessionCreationEnabled(false).buildSubject(),
                authToken,
                isSecure,
                authcScheme
        );
    }
}
