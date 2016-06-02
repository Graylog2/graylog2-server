package org.graylog2.security.realm;

import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.plugin.inject.Graylog2Module;

public class AuthenticatingRealmModule extends Graylog2Module {

    @Override
    protected void configure() {
        final MapBinder<String, AuthenticatingRealm> auth = authenticationRealmBinder();

        auth.addBinding(AccessTokenAuthenticator.NAME).to(AccessTokenAuthenticator.class).in(Scopes.SINGLETON);
        auth.addBinding(RootAccountRealm.NAME).to(RootAccountRealm.class).in(Scopes.SINGLETON);
        auth.addBinding(LdapUserAuthenticator.NAME).to(LdapUserAuthenticator.class).in(Scopes.SINGLETON);
        auth.addBinding(PasswordAuthenticator.NAME).to(PasswordAuthenticator.class).in(Scopes.SINGLETON);
        auth.addBinding(SessionAuthenticator.NAME).to(SessionAuthenticator.class).in(Scopes.SINGLETON);
    }
}
