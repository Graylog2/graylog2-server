package org.graylog2.security.realm;

import com.google.inject.multibindings.MapBinder;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.plugin.inject.Graylog2Module;

public class AuthenticatingRealmModule extends Graylog2Module {

    @Override
    protected void configure() {
        final MapBinder<String, AuthenticatingRealm> auth = authenticationRealmBinder();

        auth.addBinding(AccessTokenAuthenticator.NAME).to(AccessTokenAuthenticator.class);
        auth.addBinding(GraylogSimpleAccountRealm.NAME).to(GraylogSimpleAccountRealm.class);
        auth.addBinding(LdapUserAuthenticator.NAME).to(LdapUserAuthenticator.class);
        auth.addBinding(PasswordAuthenticator.NAME).to(PasswordAuthenticator.class);
        auth.addBinding(SessionAuthenticator.NAME).to(SessionAuthenticator.class);
    }
}
