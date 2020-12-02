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

import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.Configuration;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.security.OrderedAuthenticatingRealms;
import org.graylog2.security.StaticOrderedAuthenticatingRealms;

import java.util.Set;

public class AuthenticatingRealmModule extends Graylog2Module {

    private final Set<String> deactivatedRealms;

    public AuthenticatingRealmModule(Configuration configuration) {
        this.deactivatedRealms = configuration.getDeactivatedBuiltinAuthenticationProviders();
    }

    @Override
    protected void configure() {
        final MapBinder<String, AuthenticatingRealm> auth = authenticationRealmBinder();

        bind(OrderedAuthenticatingRealms.class).to(StaticOrderedAuthenticatingRealms.class).in(Scopes.SINGLETON);

        add(auth, AccessTokenAuthenticator.NAME, AccessTokenAuthenticator.class);
        add(auth, RootAccountRealm.NAME, RootAccountRealm.class);
        add(auth, SessionAuthenticator.NAME, SessionAuthenticator.class);
        add(auth, HTTPHeaderAuthenticationRealm.NAME, HTTPHeaderAuthenticationRealm.class);
        add(auth, UsernamePasswordRealm.NAME, UsernamePasswordRealm.class);
        add(auth, BearerTokenRealm.NAME, BearerTokenRealm.class);
    }

    private void add(MapBinder<String, AuthenticatingRealm> auth, String name,
                     Class<? extends AuthenticatingRealm> realm) {
        if (!deactivatedRealms.contains(name)) {
            auth.addBinding(name).to(realm).in(Scopes.SINGLETON);
        }
    }
}
