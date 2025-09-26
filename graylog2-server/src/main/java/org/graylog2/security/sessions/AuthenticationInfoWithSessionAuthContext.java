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
package org.graylog2.security.sessions;

import com.google.common.base.Preconditions;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * Extends an existing {@link AuthenticationInfo} with a {@link SessionAuthContext} as a means to transport session
 * attributes from a shiro authentication realm to the session store for persistence.
 * <p>
 * See {@link org.graylog2.shared.security.PersistSessionDataListener} for the listener implementation that picks up
 * this information after a successful login and persists it to the session store.
 */
public record AuthenticationInfoWithSessionAuthContext(AuthenticationInfo authenticationInfo,
                                                       SessionAuthContext sessionAuthContext) implements AuthenticationInfo {

    public AuthenticationInfoWithSessionAuthContext {
        Preconditions.checkNotNull(authenticationInfo, "authenticationInfo must not be null");
        Preconditions.checkNotNull(sessionAuthContext, "sessionAuthContext must not be null");
    }

    @Override
    public PrincipalCollection getPrincipals() {
        return authenticationInfo.getPrincipals();
    }

    @Override
    public Object getCredentials() {
        return authenticationInfo.getCredentials();
    }
}
