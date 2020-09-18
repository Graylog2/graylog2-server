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
package org.graylog2.security.realm;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog.security.authservice.AuthServiceAuthenticator;
import org.graylog.security.authservice.AuthServiceCredentials;
import org.graylog.security.authservice.AuthServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AuthServiceRealm extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(AuthServiceRealm.class);

    public static final String NAME = "auth-service";

    private final AuthServiceAuthenticator authenticator;

    @Inject
    public AuthServiceRealm(AuthServiceAuthenticator authenticator) {
        this.authenticator = authenticator;
        setAuthenticationTokenClass(UsernamePasswordToken.class);
        setCachingEnabled(false);
        // Credentials will be matched via the authentication service itself so we don't need Shiro to do it
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        if (authToken instanceof UsernamePasswordToken) {
            return doGetAuthenticationInfo((UsernamePasswordToken) authToken);
        }
        throw new UnsupportedTokenException("Unsupported authentication token type: " + authToken.getClass());
    }

    private AuthenticationInfo doGetAuthenticationInfo(UsernamePasswordToken token) throws AuthenticationException {
        final String username = token.getUsername();
        final String password = String.valueOf(token.getPassword());

        if (isNullOrEmpty(username) || isNullOrEmpty(password)) {
            LOG.error("Username or password were empty. Not attempting authentication service authentication");
            return null;
        }

        LOG.info("Attempting authentication for username <{}>", username);
        try {
            final AuthServiceResult result = authenticator.authenticate(AuthServiceCredentials.create(username, password));

            if (result.isSuccess()) {
                LOG.info("Successfully authenticated username <{}> for user profile <{}> with backend <{}/{}/{}>",
                        result.username(), result.userProfileId(), result.backendTitle(), result.backendType(), result.backendId());
                return toAuthenticationInfo(result.userProfileId());
            } else {
                LOG.warn("Failed to authenticate username <{}> with backend <{}/{}/{}>",
                        result.username(), result.backendTitle(), result.backendType(), result.backendId());
                return null;
            }
        } catch (Exception e) {
            LOG.error("Unhandled authentication error", e);
            return null;
        }
    }

    private AuthenticationInfo toAuthenticationInfo(String principal) {
        return new SimpleAccount(principal, null, NAME);
    }
}
