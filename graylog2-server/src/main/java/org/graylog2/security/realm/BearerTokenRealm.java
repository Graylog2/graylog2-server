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
package org.graylog2.security.realm;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.graylog.security.authservice.AuthServiceAuthenticator;
import org.graylog.security.authservice.AuthServiceException;
import org.graylog.security.authservice.AuthServiceResult;
import org.graylog.security.authservice.AuthServiceToken;
import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
import org.graylog2.shared.security.TypedBearerToken;
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

        setAuthenticationTokenClass(TypedBearerToken.class);
        setCachingEnabled(false);

        // Credentials will be matched via the authentication service itself so we don't need Shiro to do it
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        if (authToken instanceof TypedBearerToken) {
            return doGetAuthenticationInfo((TypedBearerToken) authToken);
        }
        throw new UnsupportedTokenException("Unsupported authentication token type: " + authToken.getClass());
    }

    private AuthenticationInfo doGetAuthenticationInfo(TypedBearerToken token) throws AuthenticationException {
        log.debug("Attempting authentication for bearer token of type <{}>.",
                token.getType());
        try {
            final AuthServiceResult result = authenticator.authenticate(AuthServiceToken.builder()
                    .token(token.getToken())
                    .type(token.getType())
                    .build());

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
        String realmName = NAME + "/" + result.backendType();

        @SuppressWarnings("ConstantConditions")
        final SimplePrincipalCollection principals = new SimplePrincipalCollection(
                ImmutableList.of(result.userProfileId(), result.sessionAttributes()), realmName);

        return new SimpleAccount(principals, null, realmName);
    }
}
