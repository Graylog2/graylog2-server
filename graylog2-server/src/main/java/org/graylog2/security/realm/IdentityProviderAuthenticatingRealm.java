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
import org.graylog.security.idp.IDPAuthResult;
import org.graylog.security.idp.IdentityProviderAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.google.common.base.Strings.isNullOrEmpty;

public class IdentityProviderAuthenticatingRealm extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(IdentityProviderAuthenticatingRealm.class);

    public static final String NAME = "idp";

    private final IdentityProviderAuthenticator authenticator;

    @Inject
    public IdentityProviderAuthenticatingRealm(IdentityProviderAuthenticator authenticator) {
        this.authenticator = authenticator;
        setAuthenticationTokenClass(UsernamePasswordToken.class);
        setCachingEnabled(false);
        // Credentials will be matched via the identity provider
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        if (authToken instanceof UsernamePasswordToken) {
            final UsernamePasswordToken token = (UsernamePasswordToken) authToken;
            try {
                return doGetAuthenticationInfo(token);
            } finally {
                // The UsernamePasswordToken documentation tells us to always call clear() after using the token to
                // perform a login attempt to make sure the password is cleared from memory.
                // How useful this is, is questionable. Shiro itself is converting the char array into a String object
                // when using a HashedCredentialsMatcher, for example. But let's do it here anyway.
                token.clear();
            }
        }
        throw new UnsupportedTokenException("Unsupported authentication token type: " + authToken.getClass());
    }

    private AuthenticationInfo doGetAuthenticationInfo(UsernamePasswordToken token) throws AuthenticationException {
        final String username = token.getUsername();
        final char[] password = token.getPassword();

        if (isNullOrEmpty(username) || (password == null || password.length < 1)) {
            LOG.error("Username or password were empty. Not attempting identity provider authentication");
            return null;
        }

        LOG.info("Attempting authentication for username <{}>", username);

        final IDPAuthResult result = authenticator.authenticate(username, password);

        if (result.isSuccess()) {
            LOG.info("Successfully authenticated username <{}> for user profile <{}>", result.username(), result.userProfileId());
            return toAuthenticationInfo(result.userProfileId());
        } else {
            LOG.warn("Failed to authenticate username <{}>", result.username());
            return null;
        }
    }

    private AuthenticationInfo toAuthenticationInfo(String principal) {
        return new SimpleAccount(principal, null, "identity-provider");
    }
}
