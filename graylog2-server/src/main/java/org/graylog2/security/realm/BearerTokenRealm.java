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
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog.security.authservice.AuthServiceAuthenticator;
import org.graylog.security.authservice.AuthServiceCredentials;
import org.graylog.security.authservice.AuthServiceException;
import org.graylog.security.authservice.AuthServiceResult;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class BearerTokenRealm extends AuthenticatingRealm {
    private static final Logger log = LoggerFactory.getLogger(BearerTokenRealm.class);

    public static final String NAME = "bearer-token";

    private final AuthServiceAuthenticator authenticator;

    @Inject
    public BearerTokenRealm(AuthServiceAuthenticator authenticator) {
        this.authenticator = authenticator;

        setAuthenticationTokenClass(BearerToken.class);
        setCachingEnabled(false);

        // Credentials will be matched via the authentication service itself so we don't need Shiro to do it
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        if (authToken instanceof BearerToken) {
            return doGetAuthenticationInfo((BearerToken) authToken);
        }
        throw new UnsupportedTokenException("Unsupported authentication token type: " + authToken.getClass());
    }

    private AuthenticationInfo doGetAuthenticationInfo(BearerToken token) throws AuthenticationException {
        log.debug("Attempting authentication for bearer token received from <{}>", token.getHost());
        try {
            final AuthServiceResult result =
                    authenticator.authenticate(AuthServiceCredentials.create("", EncryptedValue.builder().build()));

            if (result.isSuccess()) {
                log.debug("Successfully authenticated username <{}> for user profile <{}> with backend <{}/{}/{}>",
                        result.username(), result.userProfileId(), result.backendTitle(), result.backendType(),
                        result.backendId());
                return toAuthenticationInfo(result);
            } else {
                log.warn("Failed to authenticate username <{}> with backend <{}/{}/{}>",
                        result.username(), result.backendTitle(), result.backendType(), result.backendId());
                return null;
            }
        } catch (AuthServiceException e) {
            throw new AuthenticationServiceUnavailableException(e);
        } catch (Exception e) {
            log.error("Unhandled authentication error", e);
            return null;
        }
    }

    private AuthenticationInfo toAuthenticationInfo(AuthServiceResult result) {
        return new SimpleAccount(result.userProfileId(), null, NAME + "/" + result.backendType());
    }
}
