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
import org.graylog.security.authservice.AuthServiceException;
import org.graylog.security.authservice.AuthServiceResult;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class UsernamePasswordRealm extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(UsernamePasswordRealm.class);

    public static final String NAME = "username-password";

    private final AuthServiceAuthenticator authenticator;
    private final EncryptedValueService encryptedValueService;
    private final String rootUsername;

    @Inject
    public UsernamePasswordRealm(AuthServiceAuthenticator authenticator,
                            EncryptedValueService encryptedValueService,
                            @Named("root_username") String rootUsername) {
        checkArgument(!isBlank(rootUsername), "root_username cannot be null or blank");

        this.authenticator = authenticator;
        this.encryptedValueService = encryptedValueService;
        this.rootUsername = rootUsername;

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
        final String plainPassword = String.valueOf(token.getPassword());

        if (isBlank(username) || isBlank(plainPassword)) {
            LOG.error("Username or password were empty. Not attempting authentication service authentication");
            return null;
        }
        if (rootUsername.equals(username)) {
            LOG.debug("Authentication services should not handle the local admin user <{}> - skipping", username);
            return null;
        }

        LOG.debug("Attempting authentication for username <{}>", username);
        try {
            // We encrypt the password before passing it on to reduce the chance of exposing it somewhere by accident.
            final EncryptedValue encryptedPassword = encryptedValueService.encrypt(plainPassword);
            final AuthServiceResult result = authenticator.authenticate(AuthServiceCredentials.create(username, encryptedPassword));

            if (result.isSuccess()) {
                LOG.debug("Successfully authenticated username <{}> for user profile <{}> with backend <{}/{}/{}>",
                        result.username(), result.userProfileId(), result.backendTitle(), result.backendType(), result.backendId());
                return toAuthenticationInfo(result);
            } else {
                LOG.debug("Failed to authenticate username <{}> with backend <{}/{}/{}>",
                        result.username(), result.backendTitle(), result.backendType(), result.backendId());
                return null;
            }
        } catch (AuthServiceException e) {
            throw new AuthenticationServiceUnavailableException("Authentication service error", e);
        } catch (AuthenticationServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Unhandled authentication error", e);
            return null;
        }
    }

    private AuthenticationInfo toAuthenticationInfo(AuthServiceResult result) {
        return new SimpleAccount(result.userProfileId(), null, NAME + "/" + result.backendType());
    }
}
