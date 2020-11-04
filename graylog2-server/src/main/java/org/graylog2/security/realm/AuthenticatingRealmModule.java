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

        // TODO: Remove this before 4.0 - only kept to be able to test the old code
        //bind(OrderedAuthenticatingRealms.class).to(DynamicOrderedAuthenticatingRealms.class).in(Scopes.SINGLETON);
        bind(OrderedAuthenticatingRealms.class).to(StaticOrderedAuthenticatingRealms.class).in(Scopes.SINGLETON);

        add(auth, AccessTokenAuthenticator.NAME, AccessTokenAuthenticator.class);
        add(auth, RootAccountRealm.NAME, RootAccountRealm.class);
        // TODO: Remove this before 4.0 - only kept to be able to test the old code
        //add(auth, LdapUserAuthenticator.NAME, LdapUserAuthenticator.class);
        //add(auth, PasswordAuthenticator.NAME, PasswordAuthenticator.class);
        add(auth, SessionAuthenticator.NAME, SessionAuthenticator.class);
        add(auth, HTTPHeaderAuthenticationRealm.NAME, HTTPHeaderAuthenticationRealm.class);
        add(auth, AuthServiceRealm.NAME, AuthServiceRealm.class);
    }

    private void add(MapBinder<String, AuthenticatingRealm> auth, String name,
                     Class<? extends AuthenticatingRealm> realm) {
        if (!deactivatedRealms.contains(name)) {
            auth.addBinding(name).to(realm).in(Scopes.SINGLETON);
        }
    }
}
